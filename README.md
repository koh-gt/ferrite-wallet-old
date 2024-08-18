Welcome to _Bitcoin Wallet_, a standalone Bitcoin payment app for your Android device!

This project contains several sub-projects:

 * __wallet__:
     The Android app itself. This is probably what you're searching for.
 * __market__:
     App description and promo material for the Google Play app store.
 * __integration-android__:
     A tiny library for integrating Bitcoin payments into your own Android app
     (e.g. donations, in-app purchases).
 * __sample-integration-android__:
     A minimal example app to demonstrate integration of Bitcoin payments into
     your Android app.

Requirements:
Ubuntu 22.04 LTS

### gradle
```
git clone https://github.com/ferritecoin/ferrite-wallet

# need Gradle 2.10
wget https://services.gradle.org/distributions/gradle-2.10-bin.zip
sudo apt-get install unzip
sudo mkdir /opt/gradle
sudo unzip gradle-2.10-bin.zip -d /opt/gradle

# Add to PATH
sudo nano /etc/profile.d/gradle.sh
# add the lines to the file, Ctrl+S to save then Ctrl+X to exit.
export GRADLE_HOME=/opt/gradle/gradle-2.10
export PATH=${GRADLE_HOME}/bin:${PATH}
source /etc/profile.d/gradle.sh

gradle -v
```

### Java JDK
```
sudo apt update
sudo apt install openjdk-8-jdk

java -version

# if not the correct version
sudo update-alternatives --config java

  2            /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java   1081      manual mode

choose java-8
```

### Android SDK
```
# download
cd /opt
wget http://dl.google.com/android/android-sdk_r24.4.1-linux.tgz
tar -xvf android-sdk*-linux.tgz
cd android-sdk-linux/tools
./android update sdk --no-ui --filter platform,platform-tools

# set PATH
sudo nano /etc/profile.d/android.sh

Insert
export PATH=$PATH:/opt/android-sdk-linux/platform-tools
export ANDROID_TOOLS=/opt/android-sdk-linux

source /etc/profile.d/android.sh

# i386 support
sudo dpkg --add-architecture i386
sudo apt-get update
sudo apt-get install -y libc6:i386 libstdc++6:i386 zlib1g:i386

# install sdk
cd /opt/android-sdk-linux/tools
./android list sdk --all
./android update sdk --no-ui --all

# add to PATH

echo "sdk.dir=/opt/android-sdk-linux" >> local.properties

echo "export ANDROID_HOME=/opt/android-sdk-linux" >> ~/.bashrc
echo "export PATH=\$ANDROID_HOME/tools:\$ANDROID_HOME/platform-tools:\$PATH" >> ~/.bashrc

source ~/.bashrc
```

### Ferritej dependency
First compile ferritej
ferritej/core/target/ferritej-core-1.0.0.jar
copy over to your 
ferrite-wallet/sample-integration-android/ferritej-core-1.0.0.jar
```
mvn install:install-file \
  -Dfile=ferritej-core-1.0.0.jar \
  -DgroupId=org.ferritej \
  -DartifactId=ferritej-core \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```



```
cd ferrite-wallet
sudo gradle clean build

```

You can build all sub-projects at once using Gradle:

`gradle clean build`
