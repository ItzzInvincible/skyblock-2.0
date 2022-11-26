package com.skyblock.skyblock.features.entities.wolf;

import com.skyblock.skyblock.Skyblock;
import com.skyblock.skyblock.features.entities.SkyblockEntity;
import net.minecraft.server.v1_8_R3.EntityWolf;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftWolf;
import org.bukkit.entity.EntityType;

public class Wolf extends SkyblockEntity {

    private WolfType type;

    public Wolf(String type) {
        super(Skyblock.getPlugin(), EntityType.WOLF);

        this.type = WolfType.valueOf(type);

        switch (this.type){
            case RUINS:
                loadStats(250, 90, false, false, true, new Equipment(), "Wolf", 15, 15);
                break;
            case OLD_WOLF:
                loadStats(15000, 800, false, false, true, new Equipment(), "Old Wolf", 55, 45);
                break;
            case PACK_SPIRIT:
                loadStats(6000, 240, false, false, true, new Equipment(), "Pack Spirit", 30, 12);
                break;
            case HOWLING_SPIRIT:
                loadStats(7000, 400, false, false, true, new Equipment(), "Howling Spirit", 35, 22);
                break;
            case SOUL_OF_THE_ALPHA:
                loadStats(31150, 1140, false, false, true, new Equipment(), "Soul of the Alpha", 55, 50);
                break;
        }
    }

    @Override
    protected void tick() {
        org.bukkit.entity.Wolf wolf = (org.bukkit.entity.Wolf) getVanilla();

        EntityWolf nms = (((CraftWolf) wolf)).getHandle();

        nms.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.4);

        if (wolf.getTarget() != null) {
            wolf.setAngry(true);
        } else {
            wolf.setAngry(false);
        }
    }
}
