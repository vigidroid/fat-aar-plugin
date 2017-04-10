# fat-aar-plugin

[ ![Download](https://api.bintray.com/packages/vigidroid/maven/fat-aar-plugin/images/download.svg) ](https://bintray.com/vigidroid/maven/fat-aar-plugin/_latestVersion)

This is a gradle plugin that helps to output fat aar from android library. I am inspired by [android-fat-aar][1]. And aim to make more flexible and functional. It's convenient to **sdk developer**(developer that provide a single aar library).

It works with [the android gradle plugin][3], the android plugin's version of the development is `2.2.3`, other revision is not tested actually. Commit an issue as you encounter some compatibility.

**Update: Android plugin version 2.3.0 and later, is not well supported. [disable build-cache][4] may do the trick. So the recommend version is 2.2.3 and prior. Look [this issue][6] for more.**

Essentially, `fat-aar-plugin` makes a hack way, to collect resources, jar files and something others in embedded dependencies, into the bundled output aar. Click [here](#about-aar-file) to know more about `AAR`.

**[Features]**
* Support embed `android library project`, `java project`, `android library and java library from maven repositories`. Local jar file is not needed to use `embed`, `compile` is enough.
* Work fine with the original features provided by `android plugin`. Such as multi build type, product flavor, manifest placeholder, proguard... If you find something wrong, commit an issue.
* The jar files in embedded dependencies will be bundled into `libs\` in aar when the proguard is off. Otherwise , they will be bundled into `classes.jar` in aar, it means classes in dependencies will also be obfuscated.

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
        classpath 'me.vigi:fat-aar-plugin:0.2.8'
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

## About AAR File

AAR is a file format for android library.
The file itself is a zip file that containing useful stuff in android.
See [anatomy of an aar file here][2].

**support list for now:**

- [x] manifest merge
- [x] classes jar and external jars merge
- [x] res merge
- [x] R.txt merge
- [x] assets merge
- [x] jni libs merge
- [x] proguard.txt merge
- [ ] lint.jar merge
- [ ] aidl merge?
- [ ] public.txt merge?

## Known Defects or Issues

* **Proguard note.** Produce lots of(maybe) `Note: duplicate definition of library class`, while proguard is on. A workaround is to add `-dontnote` in `proguard-rules.pro`.
* **The overlay order of res merge is changed:** Embedded dependency has higher priority than other dependencies.
* **Res merge conflicts.** If the library res folder and embedded dependencies res have the same res Id(mostly `string/app_name`). A duplicate resources build exception will be thrown. To avoid res conflicts, consider using a prefix to each res Id, both in library res and aar dependencies if possible.
* [More issue or defect][5] is welcomed...

## Thanks
[android-fat-aar][1]

[1]: https://github.com/adwiv/android-fat-aar
[2]: https://developer.android.com/studio/projects/android-library.html#aar-contents
[3]: https://developer.android.com/studio/releases/gradle-plugin.html
[4]: https://developer.android.com/studio/build/build-cache.html#disable_build_cache
[5]: https://github.com/Vigi0303/fat-aar-plugin/issues
[6]: https://github.com/Vigi0303/fat-aar-plugin/issues/4
