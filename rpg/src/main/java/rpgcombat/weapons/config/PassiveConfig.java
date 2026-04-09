package rpgcombat.weapons.config;

import java.util.Map;

/**
 * DTO per a una passiva definida al JSON.
 */
public record PassiveConfig(String type, Map<String, Object> params){}
