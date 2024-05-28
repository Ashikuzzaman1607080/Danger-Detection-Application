# Objective 

Since there have been more and more incidents of women being harassed in the recent past, girls need to think twice before going out of their houses. To ensure their safety we developed an Android-based automated system to detect danger for women and children using audio from the surroundings. As the Android phone is available to everyone nowadays, they focused on using this device rather than developing a system on some external hardware. Different signal processing methods with deep learning techniques are used for this work. This work also addresses noise from the environment for any chaos and nullifies them using different noise reduction techniques. For this application, we used a Butterworth high-pass filter. The Android application also takes necessary action based on the setting when any unfavorable situation is detected. Android device users can use this application without any cost, which will pave the way to ensure the safety of women and children.

# Features of the Application

This app will collect features from the surroundings and predict the situation. Key features include:

* **Audio Collection:** It will continuously track audio from the surroundings and try to understand the situation.
* **Deep Learning Classification:** Analyzing the situation from the audio of the surroundings.
* **Location Tracking:** When any unfavorable situation is detected, the app automatically fetches the location and sends it with SMS.
* **Communication System:** Based on the situation and setting of this app will make a call or send a message including the location information.


# Components Used
This project utilizes various components and technologies, including:

* **Android Studio:** Integrated Development Environment (IDE) for Android app development. 
* **Java:** Programming language used for developing Android apps.
* **REST API:** API to send audio to the server and return the analyzed result.
* **Python:** A Python backend server to process the audio collected from the Android application.
* **Tensorflow:** An open-source machine learning platform to analyze the audio.
* **Google Fused Location Services:** API for retrieving accurate location information on Android devices.

# Demo-Application

https://github.com/Ashikuzzaman1607080/Danger-Detection-Application/assets/50808571/78d8cfca-be11-4c84-a0ce-4c5541841f68


# Work progress

This repository consists of the original code for the application. Clone this repository and run it with Android Studio. APK can be provided based on request.

# Python Server

Check out the following two repositories to better understand the backend Python server for the online version:

* [Danger-detection](https://github.com/awal-ahmed/Danger-detection)
* [AudioViT](https://github.com/awal-ahmed/AudioViT)

# Contract us:

For any questions or queries contact us:
* [Md. Ashikuzzaman](ashik.kanon683@gmail.com)
* [Awal Ahmed Fime](awalahmedfime984@gmail.com)
