#!/bin/sh
# Run the packaged MIDlet in MicroEmulator on a virtual X display and drive it
# with synthetic key events, saving a screenshot at each step.
#
# EMU_PHASE=2 runs the key probe instead of the playthrough: it reports, for
# each host key, how much of the screen moved when it was pressed, which is
# how the mapping between the host keyboard and the phone's keypad was
# established rather than guessed.
set -eu
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

LIB=lib
CP=$LIB/microemu-javase-swing-2.0.4.jar:$LIB/microemu-javase-2.0.4.jar
CP=$CP:$LIB/microemu-cldc-2.0.4.jar:$LIB/microemu-midp-2.0.4.jar
CP=$CP:$LIB/microemu-injected-2.0.4.jar:$LIB/asm-3.1.jar
CP=$CP:$LIB/microemu-jsr-135-2.0.4.jar

mkdir -p shots
PHASE=${EMU_PHASE:-1}

cat > build/emu-run.sh <<INNER
#!/bin/sh
java -cp $CP org.microemu.app.Main --resizableDevice 240 320 dist/VanguardZero.jad \
    > shots/emulator.log 2>&1 &
EMU=\$!
java -cp build/tools EmuDrive shots ${EMU_WAIT:-6000} $PHASE \
    > shots/drive.log 2>&1
RC=\$?
sleep 1
kill \$EMU 2>/dev/null || true
wait \$EMU 2>/dev/null || true
exit \$RC
INNER
chmod +x build/emu-run.sh

FAIL=0
# The driver's own exit code matters: it fails when a control the script
# pressed did nothing, so it must not be swallowed by a pipe.
xvfb-run -a -s "-screen 0 800x600x24" build/emu-run.sh || FAIL=1
grep -v JAVA_TOOL_OPTIONS shots/drive.log || true

echo "--- emulator log ---"
grep -v JAVA_TOOL_OPTIONS shots/emulator.log | tail -25
# This container has no sound card, so MicroEmulator's tone generator throws.
# The game guards every tone call, so those lines are expected noise here.
if grep -E 'Exception|Error' shots/emulator.log \
        | grep -v JAVA_TOOL_OPTIONS \
        | grep -viE 'SourceDataLine|javax.sound|PCTone|playTone|AudioSystem' \
        > /dev/null; then
    echo "!!! the emulator log contains unexpected errors"
    grep -E 'Exception|Error' shots/emulator.log | grep -viE 'SourceDataLine|javax.sound|PCTone|playTone|AudioSystem' | head -20
    FAIL=1
fi

if [ "$FAIL" != "0" ]; then
    echo "!!! the emulator pass failed"
    exit 1
fi
echo "the emulator pass is clean: no unexpected errors, and every control the"
echo "script pressed changed the screen"
