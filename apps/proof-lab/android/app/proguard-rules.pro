# kotlinx.serialization keeps its generated serializers on the companion; R8 can't see the
# reflective link, so keep them for the model classes we persist.
-keepclassmembers class com.blahmonster.prooflab.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.blahmonster.prooflab.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.blahmonster.prooflab.core.**$$serializer { *; }
