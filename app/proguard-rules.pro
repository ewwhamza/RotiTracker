# Item types are stored in the database by their enum constant name ("ROTI", "CHAPATI") and read
# back with valueOf. If R8 renamed these constants, every previously saved row would fail to load,
# so keep them verbatim.
-keepclassmembers enum com.ansar.rotitrack.data.ItemType {
    *;
}

# Room looks up its generated implementation class by name at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
