#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status.

echo
echo
echo Running unit and Android tests
echo "====================================================================="

./gradlew :app:assembleDebug -PdisablePreDex | sed "s@^@$name @"  # Prefix every line with directory
code=${PIPESTATUS[0]}
if [ "$code" -ne "0" ]; then
exit $code
fi

./gradlew :app:assembleAndroidTest -PdisablePreDex | sed "s@^@$name @"  # Prefix every line with directory
./gradlew test -PdisablePreDex | sed "s@^@$name @"  # Prefix every line with directory

apkfile=app/build/outputs/apk/mock/debug/app-mock-debug.apk
testapkfile=app/build/outputs/apk/androidTest/mock/debug/app-mock-debug-androidTest.apk
if [ ! -f $apkfile ] || [ ! -f $testapkfile ] ; then
echo "APKs not found, probably due to project using multiple flavors. Skipping $name"
popd > /dev/null  # Silent popd
continue
fi
echo "Sending APKs to Firebase..."
echo "y" | /usr/bin/gcloud firebase test android run --app $apkfile --test $testapkfile --device model=taimen,version=27 --device model=sailfish,version=28, --device model=g3,version=19 --results-bucket=android-architecture-test-results --results-dir=$CIRCLE_BUILD_NUM/$name

code=${PIPESTATUS[0]}
if [ "$code" -ne "0" ]; then
exit $code
fi
