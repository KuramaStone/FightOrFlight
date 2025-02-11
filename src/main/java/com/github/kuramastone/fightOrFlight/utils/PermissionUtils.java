package com.github.kuramastone.fightOrFlight.utils;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Does not use imports for luckperms to avoid
 */
public class PermissionUtils {

    /**
     * Check if a Player with this uuid has this permission. If the player is offline, it polls LuckPerms.
     *
     * @return True if they have permission.
     */
    public static boolean hasPermission(ServerPlayer serverPlayer, String permission) {
        return Permissions.check(serverPlayer, permission);
    }


}
