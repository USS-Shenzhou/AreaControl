package org.teacon.areacontrol.impl;

import org.teacon.areacontrol.api.Area;

public class AreaMath {

    public enum SetRelation {
        INDEPENDENT, SUPERSET, INTERSECT, SAME, SUBSET;

        public SetRelation flip() {
            return switch (this) {
                case INDEPENDENT -> INDEPENDENT;
                case SUPERSET -> SUBSET;
                case INTERSECT -> INTERSECT;
                case SAME -> SAME;
                case SUBSET -> SUPERSET;
            };
        }
    }

    public static boolean isEnclosing(Area parent, Area maybeChild) {
        return parent.minX <= maybeChild.minX && maybeChild.maxX <= parent.maxX
                && parent.minY <= maybeChild.minY && maybeChild.maxY <= parent.maxY
                && parent.minZ <= maybeChild.minZ && maybeChild.maxZ <= parent.maxZ;
    }

    public static boolean isCoveringSameArea(Area a, Area b) {
        return a.minX == b.minX && a.minY == b.minY && a.minZ == b.minZ && a.maxX == b.maxX && a.maxY == b.maxY && a.maxZ == b.maxZ;
    }

    public static SetRelation relationBetween(Area left, Area right) {
        return relationBetween(left.minX, left.minY, left.minZ, left.maxX, left.maxY, left.maxZ,
                right.minX, right.minY, right.minZ, right.maxX, right.maxY, right.maxZ);
    }

    public static SetRelation relationBetween(int aMinX, int aMinY, int aMinZ, int aMaxX, int aMaxY, int aMaxZ, Area right) {
        return relationBetween(aMinX, aMinY, aMinZ, aMaxX, aMaxY, aMaxZ,
                right.minX, right.minY, right.minZ, right.maxX, right.maxY, right.maxZ);
    }

    public static SetRelation relationBetween(Area left, int bMinX, int bMinY, int bMinZ, int bMaxX, int bMaxY, int bMaxZ) {
        return relationBetween(left.minX, left.minY, left.minZ, left.maxX, left.maxY, left.maxZ,
                bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ);
    }

    /**
     * Determine the relation between area A and area B.
     * @param aMinX Smaller X of area A
     * @param aMinY Smaller Y of area A
     * @param aMinZ Smaller Z of area A
     * @param aMaxX Larger X of area A
     * @param aMaxY Larger Y of area A
     * @param aMaxZ Larger Z of area A
     * @param bMinX Smaller X of area B
     * @param bMinY Smaller Y of area B
     * @param bMinZ Smaller Z of area B
     * @param bMaxX Larger X of area B
     * @param bMaxY Larger Y of area B
     * @param bMaxZ Larger Z of area B
     * @return The relation for area A being related to area B
     */
    public static SetRelation relationBetween(
            int aMinX, int aMinY, int aMinZ, int aMaxX, int aMaxY, int aMaxZ,
            int bMinX, int bMinY, int bMinZ, int bMaxX, int bMaxY, int bMaxZ
    ) {
        boolean xOverlap = (aMinX <= bMinX && bMinX <= aMaxX) || (aMinX <= bMaxX && bMaxX <= aMaxX);
        boolean yOverlap = (aMinY <= bMinY && bMinY <= aMaxY) || (aMinY <= bMaxY && bMaxY <= aMaxY);
        boolean zOverlap = (aMinZ <= bMinZ && bMinZ <= aMaxZ) || (aMinZ <= bMaxZ && bMaxZ <= aMaxZ);
        if (xOverlap && yOverlap && zOverlap) {
            if (aMinX == bMinX && aMinY == bMinY & aMinZ == bMinZ && aMaxX == bMaxX && aMaxY == bMaxY && aMaxZ == bMaxZ) {
                return SetRelation.SAME;
            } else if (aMinX < bMinX && bMaxX < aMaxX
                    && aMinY < bMinY && bMaxY < aMaxY
                    && aMinZ < bMinZ && bMaxZ < aMaxZ) {
                return SetRelation.SUPERSET;
            } else {
                return SetRelation.INTERSECT;
            }
        } else if (bMinX < aMinX && aMaxX < bMaxX
                && bMinY < aMinY && aMaxY < bMaxY
                && bMinZ < aMinZ && aMaxZ < bMaxZ) {
            return SetRelation.SUBSET;
        } else {
            return SetRelation.INDEPENDENT;
        }
    }
}
