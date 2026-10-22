package com.truenins.voiddomain.core;

import com.truenins.voiddomain.platform.Numbers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DomainVolume {
   public static final int SHRINE = 0;
   public static final int BARRAGE = 1;
   public static final int UNLIMITED_VOID = 2;
   public static final float DEFAULT_SHRINE_RADIUS = 64.0F;
   public static final float DEFAULT_VOID_RADIUS = 40.0F;

   private DomainVolume() {
   }

   public static int maxRadius(int kind) {
      return kind == 0 ? 80 : 50;
   }

   public static boolean encloses(Vec3 outer, double outerRadius, Vec3 inner, double innerRadius) {
      return innerRadius > 0.0D && outerRadius > innerRadius && outer.distanceTo(inner) + innerRadius < outerRadius - 0.05D;
   }

   public static boolean contains(Vec3 center, double radius, Vec3 point) {
      return radius > 0.0D && center.distanceToSqr(point) < radius * radius;
   }

   public static boolean validKind(int kind) {
      return kind >= 0 && kind <= 2;
   }

   public static boolean overlaps(Vec3 a, double radiusA, Vec3 b, double radiusB) {
      double reach = radiusA + radiusB;
      return radiusA > 0.0D && radiusB > 0.0D && a.distanceToSqr(b) < reach * reach;
   }

   public static float expansion(int kind, float ageSeconds) {
      if (kind == 1) {
         return 1.0F;
      } else {
         float t = kind == 2 ? ageSeconds / 0.2F : (ageSeconds - 0.4F) / 1.8F;
         t = Numbers.clamp(t, 0.0F, 1.0F);
         return t * t * (3.0F - 2.0F * t);
      }
   }

   public static float freezeRadius(int kind, float radius, float ageSeconds) {
      return kind == 2 ? radius : radius * expansion(kind, ageSeconds);
   }

   public static boolean intersects(Vec3 center, double radius, AABB bounds) {
      if (radius <= 0.0D) {
         return false;
      } else {
         double x = Numbers.clamp(center.x, bounds.minX, bounds.maxX) - center.x;
         double y = Numbers.clamp(center.y, bounds.minY, bounds.maxY) - center.y;
         double z = Numbers.clamp(center.z, bounds.minZ, bounds.maxZ) - center.z;
         return x * x + y * y + z * z <= radius * radius;
      }
   }
}
