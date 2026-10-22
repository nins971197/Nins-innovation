package com.truenins.spell.orb;

import com.truenins.TrueNinsMod;
import com.truenins.entity.orb.PurpleOrbEntity;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keep Forge's cancellable explosion and terrain hooks, but apply cursed spell damage once. */
@Mod.EventBusSubscriber(modid = TrueNinsMod.MODID)
public final class PurpleExplosionEvents {
    private PurpleExplosionEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void detonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof PurpleOrbEntity purple) {
            purple.strikeExplosionTargets(event.getAffectedEntities());
            // The technique already applied its configured damage, including owner immunity.
            event.getAffectedEntities().clear();
        }
    }
}
