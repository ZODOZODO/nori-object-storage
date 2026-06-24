rootProject.name = "nori-object-storage"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":nori-object-storage-api")
include(":nori-object-storage-s3-starter")
