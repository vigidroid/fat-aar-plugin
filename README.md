# fat-aar-plugin
[WIP] This is a gradle plugin that helps to output fat aar from android library.

It's inspired by [android-fat-aar](https://github.com/adwiv/android-fat-aar). And intent to make more flexible and functional.

It's convenient to **sdk developer**(developer that provide a single aar library).

## Getting Started

#### Step 1: Apply plugin

Add snippet below to your root build script file:

```gradle
buildscript {
    repositories {
        maven {
            url  "http://dl.bintray.com/vigidroid/maven"
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:xxx'
        classpath 'me.vigi:fat-aar-plugin:0.0.1'
    }
}
```

Add snippet below to the `build.gradle` of your android library:

```gradle
apply plugin: 'me.vigi.fat-aar'
```

#### Step 2: Embed dependencies

change `compile` to `embed` while you want to embed the dependency in the library. Like this:

```gradle
dependencies {
    // aar project
    embed project(':aar-lib')
    // java project
    embed project(':java-lib')
    // java dependency
    embed 'com.google.guava:guava:20.0'
    // aar dependency
    embed 'com.android.volley:volley:1.0.0'
  
    // other dependencies you don't want to embed in
    compile 'com.squareup.okhttp3:okhttp:3.6.0'
}
```

**More usage see [example](./example-android).**

## Known Defects or Issues

* To aar-type dependency, only jar file in aar bundle is packaged. More entries will be supported with high priority. See [anatomy of an aar file](https://developer.android.com/studio/projects/android-library.html#aar-contents)

## Thanks
[android-fat-aar](https://github.com/adwiv/android-fat-aar)