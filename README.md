# fat-aar-plugin
[WIP] a gradle plugin that helps to output fat aar from android library

## Apply Plugin

add classpath to your root build script file:

    buildscript {
        dependencies {
            classpath 'com.android.tools.build:gradle:xxx'
            classpath 'me.vigi:fat-aar-plugin:0.0.1'
        }
    }

In the `build.gradle` of your android library, add this:

    apply plugin: 'me.vigi.fat-aar'

## Usage

change `compile` to `embed` while you want to embed the dependency in the library. Like this:

    dependencies {
      embed project(':aar-lib')
      embed 'com.google.guava:guava:20.0'
      embed 'com.android.volley:volley:1.0.0'
      
      compile 'com.squareup.okhttp3:okhttp:3.6.0'
    }

## Thanks to

[android-fat-aar](https://github.com/adwiv/android-fat-aar)
