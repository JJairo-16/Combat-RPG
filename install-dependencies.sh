#!/usr/bin/env bash

set -o pipefail

LIBS_FOLDER="libs"
CONFIG_FILE="artifacts.json"
STOP_ON_ERROR=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --libs-folder)
      LIBS_FOLDER="$2"
      shift 2
      ;;
    --config-file)
      CONFIG_FILE="$2"
      shift 2
      ;;
    --stop-on-error)
      STOP_ON_ERROR=true
      shift
      ;;
    -h|--help)
      cat <<EOF
Ús:
  $0 [opcions]

Opcions:
  --libs-folder <ruta>     Carpeta de llibreries (per defecte: libs)
  --config-file <fitxer>   Fitxer JSON de configuració (per defecte: artifacts.json)
  --stop-on-error          Aturar-se en el primer error
  -h, --help               Mostrar aquesta ajuda
EOF
      exit 0
      ;;
    *)
      echo "[ERROR] Opció no reconeguda: $1" >&2
      exit 1
      ;;
  esac
done

write_section() {
  local text="$1"
  echo
  printf '=%.0s' {1..70}
  echo
  echo "  $text"
  printf '=%.0s' {1..70}
  echo
}

write_info() {
  echo "[INFO] $1"
}

write_ok() {
  echo "[OK]   $1"
}

write_warn() {
  echo "[AVÍS] $1"
}

write_err() {
  echo "[ERROR] $1" >&2
}

test_command_exists() {
  command -v "$1" >/dev/null 2>&1
}

resolve_libs_path() {
  local relative_path="$1"
  local script_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  echo "$script_dir/$relative_path"
}

load_artifact_config() {
  local config_path="$1"

  if [[ ! -f "$config_path" ]]; then
    write_err "No s'ha trobat el fitxer de configuració: $config_path"
    return 1
  fi

  if ! jq empty "$config_path" >/dev/null 2>&1; then
    write_err "El JSON està buit o no és vàlid: $config_path"
    return 1
  fi

  local artifacts_count
  artifacts_count="$(jq '.artifacts | if type=="array" then length else 0 end' "$config_path")"

  if [[ "$(jq '.defaults == null' "$config_path")" == "true" ]]; then
    write_warn "No s'ha trobat la secció 'defaults'. S'utilitzaran només els valors per artefacte."
  fi

  if [[ "$artifacts_count" -eq 0 ]]; then
    write_err "No s'han trobat artefactes a la propietat 'artifacts'."
    return 1
  fi
}

get_json_value() {
  local config_path="$1"
  local file_name="$2"
  local field="$3"

  jq -r --arg file "$file_name" --arg field "$field" '
    .artifacts[]
    | select(.file == $file)
    | .[$field] // empty
  ' "$config_path" | head -n1
}

get_default_value() {
  local config_path="$1"
  local field="$2"

  jq -r --arg field "$field" '
    .defaults[$field] // empty
  ' "$config_path"
}

artifact_exists() {
  local config_path="$1"
  local file_name="$2"

  jq -e --arg file "$file_name" '
    .artifacts[] | select(.file == $file)
  ' "$config_path" >/dev/null 2>&1
}

install_jar() {
  local jar_path="$1"
  local config_path="$2"

  local file_name
  file_name="$(basename "$jar_path")"
  local base_name
  base_name="${file_name%.jar}"

  local artifact_group_id artifact_artifact_id artifact_version artifact_packaging artifact_generate_pom
  local default_group_id default_artifact_id default_version default_packaging default_generate_pom

  artifact_group_id="$(get_json_value "$config_path" "$file_name" "groupId")"
  artifact_artifact_id="$(get_json_value "$config_path" "$file_name" "artifactId")"
  artifact_version="$(get_json_value "$config_path" "$file_name" "version")"
  artifact_packaging="$(get_json_value "$config_path" "$file_name" "packaging")"
  artifact_generate_pom="$(get_json_value "$config_path" "$file_name" "generatePom")"

  default_group_id="$(get_default_value "$config_path" "groupId")"
  default_artifact_id="$(get_default_value "$config_path" "artifactId")"
  default_version="$(get_default_value "$config_path" "version")"
  default_packaging="$(get_default_value "$config_path" "packaging")"
  default_generate_pom="$(get_default_value "$config_path" "generatePom")"

  local group_id artifact_id version packaging
  group_id="${artifact_group_id:-$default_group_id}"
  artifact_id="${artifact_artifact_id:-${default_artifact_id:-$base_name}}"
  version="${artifact_version:-$default_version}"
  packaging="${artifact_packaging:-${default_packaging:-jar}}"

  if [[ -z "$group_id" ]]; then
    write_err "Falta groupId per a '$file_name'."
    return 1
  fi

  if [[ -z "$artifact_id" ]]; then
    write_err "Falta artifactId per a '$file_name'."
    return 1
  fi

  if [[ -z "$version" ]]; then
    write_err "Falta version per a '$file_name'."
    return 1
  fi

  write_info "Instal·lant $file_name"
  echo "       groupId    = $group_id"
  echo "       artifactId = $artifact_id"
  echo "       version    = $version"
  echo "       packaging  = $packaging"

  local mvn_args=(
    install:install-file
    "-Dfile=$jar_path"
    "-DgroupId=$group_id"
    "-DartifactId=$artifact_id"
    "-Dversion=$version"
    "-Dpackaging=$packaging"
  )

  if [[ "$artifact_generate_pom" == "true" ]] || { [[ -z "$artifact_generate_pom" ]] && [[ "$default_generate_pom" == "true" ]]; }; then
    mvn_args+=("-DgeneratePom=true")
  fi

  mvn "${mvn_args[@]}"
  local exit_code=$?

  if [[ $exit_code -ne 0 ]]; then
    write_err "Maven ha retornat el codi $exit_code en instal·lar '$file_name'."
    return 1
  fi

  write_ok "$file_name instal·lat correctament."
  return 0
}

main() {
  write_section "Instal·lador de JARs a Maven"

  if ! test_command_exists "mvn"; then
    write_err "No s'ha trobat la comanda 'mvn'. Assegura't que Maven està instal·lat i al PATH."
    exit 1
  fi

  if ! test_command_exists "jq"; then
    write_err "No s'ha trobat la comanda 'jq'. Instal·la-la per poder llegir el JSON."
    exit 1
  fi

  local libs_path
  libs_path="$(resolve_libs_path "$LIBS_FOLDER")"
  local config_path="$libs_path/$CONFIG_FILE"

  write_info "Directori de llibreries: $libs_path"
  write_info "Fitxer de configuració: $config_path"

  if [[ ! -d "$libs_path" ]]; then
    write_err "No existeix el directori de llibreries: $libs_path"
    exit 1
  fi

  load_artifact_config "$config_path" || exit 1

  shopt -s nullglob
  local jar_files=("$libs_path"/*.jar)
  shopt -u nullglob

  if [[ ${#jar_files[@]} -eq 0 ]]; then
    write_warn "No s'han trobat fitxers .jar a '$libs_path'."
    exit 0
  fi

  IFS=$'\n' jar_files=($(printf '%s\n' "${jar_files[@]}" | sort))
  unset IFS

  write_info "JARs trobats: ${#jar_files[@]}"

  local success_count=0
  local error_count=0
  local index=0
  local total=${#jar_files[@]}

  for jar in "${jar_files[@]}"; do
    ((index++))
    local jar_name
    jar_name="$(basename "$jar")"
    local percent=$(( index * 100 / total ))

    echo "[PROGRÉS] $jar_name ($index/$total) - $percent%"

    if ! artifact_exists "$config_path" "$jar_name"; then
      ((error_count++))
      write_err "No hi ha configuració per a '$jar_name' al JSON."

      if [[ "$STOP_ON_ERROR" == "true" ]]; then
        exit 1
      fi

      continue
    fi

    if install_jar "$jar" "$config_path"; then
      ((success_count++))
    else
      ((error_count++))
      if [[ "$STOP_ON_ERROR" == "true" ]]; then
        exit 1
      fi
    fi
  done

  write_section "Resum"
  write_ok "Instal·lats correctament: $success_count"

  if [[ $error_count -gt 0 ]]; then
    write_warn "Errors: $error_count"
    exit 1
  else
    write_ok "Procés completat sense errors."
  fi
}

main