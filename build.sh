#!/bin/sh
# Vanguard Zero - build driver for a J2ME (MIDP 2.0 / CLDC 1.1) MIDlet.
#
# There is no Wireless Toolkit or `preverify` binary in this environment, only a
# modern JDK. The pipeline therefore is:
#
#   1. compile hand-written CLDC 1.1 API stubs  -> build/cldc-stubs.jar
#   2. compile the game with those stubs as the -bootclasspath, so javac itself
#      rejects anything outside the CLDC subset (string '+', autoboxing, ...)
#   3. run ProGuard with -microedition -target 1.3, which shrinks, obfuscates
#      AND preverifies (writes CLDC StackMap attributes, class major version 47)
#   4. write the .jad with the exact jar size
#
# Usage: ./build.sh [deps|stubs|compile|check|jar|shrink|jad|verify|all|tools|test|shots|emu|clean]
# With no argument it runs `all`.

set -eu

ROOT=$(cd "$(dirname "$0")" && pwd)
cd "$ROOT"

LIB=lib
BUILD=build
DIST=dist
NAME=VanguardZero
MIDP=$LIB/microemu-midp-2.0.4.jar
STUBS=$BUILD/cldc-stubs.jar
PROGUARD=$LIB/proguard-base-6.2.2.jar
MAVEN=${MAVEN_REPO:-https://repo1.maven.org/maven2}

# Classes that must stay free of javax.microedition.* so the desktop harness
# (tools/) can compile and run them on a normal JDK.
CORE_CLASSES="FX Level Levels Raycaster Textures Sprites Entity World Player Balance Text"

say() { printf '==> %s\n' "$*"; }

fetch() { # fetch <maven path> <target file>
    [ -f "$2" ] && return 0
    i=1
    while [ $i -le 5 ]; do
        if curl -sSfL -o "$2.part" "$MAVEN/$1"; then
            mv "$2.part" "$2"
            printf '    fetched %s\n' "$2"
            return 0
        fi
        rm -f "$2.part"
        sleep $((i * 3))
        i=$((i + 1))
    done
    echo "ERROR: could not download $MAVEN/$1" >&2
    return 1
}

cmd_deps() {
    say "dependencies"
    mkdir -p $LIB
    fetch org/microemu/microemu-midp/2.0.4/microemu-midp-2.0.4.jar               $LIB/microemu-midp-2.0.4.jar
    fetch org/microemu/microemu-cldc/2.0.4/microemu-cldc-2.0.4.jar               $LIB/microemu-cldc-2.0.4.jar
    fetch org/microemu/microemu-javase/2.0.4/microemu-javase-2.0.4.jar           $LIB/microemu-javase-2.0.4.jar
    fetch org/microemu/microemu-javase-swing/2.0.4/microemu-javase-swing-2.0.4.jar $LIB/microemu-javase-swing-2.0.4.jar
    fetch org/microemu/microemu-injected/2.0.4/microemu-injected-2.0.4.jar       $LIB/microemu-injected-2.0.4.jar
    fetch org/microemu/microemu-jsr-135/2.0.4/microemu-jsr-135-2.0.4.jar         $LIB/microemu-jsr-135-2.0.4.jar
    fetch asm/asm/3.1/asm-3.1.jar                                                $LIB/asm-3.1.jar
    fetch net/sf/proguard/proguard-base/6.2.2/proguard-base-6.2.2.jar            $LIB/proguard-base-6.2.2.jar
}

cmd_stubs() {
    say "CLDC 1.1 API stubs"
    rm -rf $BUILD/stub-classes
    mkdir -p $BUILD/stub-classes
    # Empty -bootclasspath: the stubs are compiled only against each other.
    find stubs/src -name '*.java' > $BUILD/stub-sources.txt
    javac -source 8 -target 8 -Xlint:-options -nowarn \
          -bootclasspath $BUILD/stub-classes \
          -d $BUILD/stub-classes @$BUILD/stub-sources.txt
    (cd $BUILD/stub-classes && jar cf ../cldc-stubs.jar .)
}

cmd_compile() {
    say "compile src/vz"
    [ -f $MIDP ] || cmd_deps
    [ -f $STUBS ] || cmd_stubs
    rm -rf $BUILD/classes
    mkdir -p $BUILD/classes
    javac -source 8 -target 8 -Xlint:-options \
          -bootclasspath "$STUBS:$MIDP" \
          -d $BUILD/classes src/vz/*.java
}

cmd_check() {
    say "check MIDP-free core classes"
    rc=0
    for c in $CORE_CLASSES; do
        f=$BUILD/classes/vz/$c.class
        [ -f "$f" ] || continue
        if javap -c -p "$f" | grep -q 'javax/microedition'; then
            echo "ERROR: vz.$c references javax.microedition (must stay portable)" >&2
            rc=1
        fi
    done
    [ $rc -eq 0 ] && echo "    core classes are MIDP-free"
    return $rc
}

cmd_jar() {
    say "package build/vz-raw.jar"
    mkdir -p $BUILD
    cp manifest.txt $BUILD/MANIFEST.MF
    rm -f $BUILD/vz-raw.jar
    jar cfm $BUILD/vz-raw.jar $BUILD/MANIFEST.MF -C $BUILD/classes . -C res .
}

cmd_shrink() {
    say "shrink + obfuscate + preverify (ProGuard, -microedition -target 1.3)"
    [ -f $PROGUARD ] || cmd_deps
    mkdir -p $DIST
    rm -f $DIST/$NAME.jar
    java -jar $PROGUARD @proguard.cfg
}

cmd_jad() {
    say "write $DIST/$NAME.jad"
    size=$(wc -c < $DIST/$NAME.jar | tr -d ' ')
    {
        cat manifest.txt
        echo "MIDlet-Jar-URL: $NAME.jar"
        echo "MIDlet-Jar-Size: $size"
    } > $DIST/$NAME.jad
}

cmd_verify() {
    say "verify CLDC bytecode"
    rm -rf $BUILD/verify
    mkdir -p $BUILD/verify
    (cd $BUILD/verify && unzip -qo ../../$DIST/$NAME.jar)
    rc=0
    n=0
    for c in $(find $BUILD/verify -name '*.class'); do
        n=$((n + 1))
        v=$(javap -v "$c" | sed -n 's/.*major version: \([0-9]*\).*/\1/p' | head -1)
        if [ "$v" != "47" ]; then
            echo "ERROR: $c has class major version $v, expected 47" >&2
            rc=1
        fi
        if javap -v "$c" | grep -q 'StackMapTable'; then
            echo "ERROR: $c has a J2SE StackMapTable instead of a CLDC StackMap" >&2
            rc=1
        fi
    done
    size=$(wc -c < $DIST/$NAME.jar | tr -d ' ')
    echo "    $n classes, all major version 47"
    echo "    jar size: $size bytes"
    if [ "$size" -gt 1048576 ]; then
        echo "ERROR: jar exceeds the 1 MB budget" >&2
        rc=1
    fi
    return $rc
}

cmd_tools() {
    cmd_compile
    cmd_check
    say "compile desktop harness (tools/)"
    rm -rf $BUILD/tools
    mkdir -p $BUILD/tools
    javac -nowarn -cp $BUILD/classes -d $BUILD/tools tools/src/*.java
}

cmd_test() {
    cmd_tools
    say "logic tests"
    java -cp "$BUILD/tools:$BUILD/classes" LogicTest
}

cmd_shots() {
    cmd_tools
    say "render frames to shots/"
    mkdir -p shots
    java -cp "$BUILD/tools:$BUILD/classes" FrameDump shots
}

cmd_emu() {
    cmd_all
    say "run in MicroEmulator (Xvfb) and take screenshots"
    cmd_tools
    mkdir -p shots
    sh tools/emu.sh
}

cmd_clean() {
    say "clean"
    rm -rf $BUILD shots
}

cmd_all() {
    cmd_deps
    cmd_stubs
    cmd_compile
    cmd_check
    cmd_jar
    cmd_shrink
    cmd_jad
    cmd_verify
    say "done: $DIST/$NAME.jar + $DIST/$NAME.jad"
}

case "${1:-all}" in
    deps)    cmd_deps ;;
    stubs)   cmd_stubs ;;
    compile) cmd_compile ;;
    check)   cmd_check ;;
    jar)     cmd_jar ;;
    shrink)  cmd_shrink ;;
    jad)     cmd_jad ;;
    verify)  cmd_verify ;;
    tools)   cmd_tools ;;
    test)    cmd_test ;;
    shots)   cmd_shots ;;
    emu)     cmd_emu ;;
    clean)   cmd_clean ;;
    all)     cmd_all ;;
    *) echo "usage: $0 [deps|stubs|compile|check|jar|shrink|jad|verify|all|tools|test|shots|emu|clean]" >&2; exit 2 ;;
esac
