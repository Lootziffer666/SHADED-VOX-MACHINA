package com.google.ar.core;

/* JADX INFO: compiled from: Session.java */
/* JADX INFO: loaded from: classes.dex */
enum aj {
    BASE_TRACKABLE(1095893248, Trackable.class),
    UNKNOWN_TO_JAVA(-1, null),
    PLANE(1095893249, Plane.class),
    POINT(1095893250, Point.class),
    AUGMENTED_IMAGE(1095893252, AugmentedImage.class),
    FACE(1095893253, AugmentedFace.class),
    STREETSCAPE_GEOMETRY(1095893251, StreetscapeGeometry.class),
    EARTH(1095893257, Earth.class),
    DEPTH_POINT(1095893265, DepthPoint.class),
    INSTANT_PLACEMENT_POINT(1095893266, InstantPlacementPoint.class);

    final int k;
    private final Class l;

    aj(int i, Class cls) {
        this.k = i;
        this.l = cls;
    }

    public static aj a(Class cls) {
        for (aj ajVar : values()) {
            Class cls2 = ajVar.l;
            if (cls2 != null && cls2.equals(cls)) {
                return ajVar;
            }
        }
        return UNKNOWN_TO_JAVA;
    }
}
