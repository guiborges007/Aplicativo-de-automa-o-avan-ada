plugins {
    alias(libs.plugins.androidLibrary )
    id("maven-publish")
}


android {
    namespace = "br.com.guilhermeborges.MinhaBiblioteca"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-maps:18.2.0@aar")
    implementation ("com.google.code.gson:gson:2.8.9") // Use a versão mais recente disponível
    implementation ("com.google.android.material:material:1.4.0")
}


publishing {
    publications {
        register("TratamentoDeRegioesECripitografia", MavenPublication::class) {
            groupId = "br.com.guilhermeborges"
            artifactId = "regioes_e_criptografia"
            version = "1.0"
            artifact("$buildDir/outputs/aar/MinhaBiblioteca-debug.aar")
        }
    }

    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/guiborges007/Aplicativo-de-automacao-avancada")
                    credentials {
                         username = project.findProperty("usuario") as String? ?: ""
                         password = project.findProperty("token") as String? ?: ""
            }
        }
    }
}


