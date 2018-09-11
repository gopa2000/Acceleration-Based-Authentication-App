# Acceleration Based Authentication App

This app is to demonstrate an authentication scheme that uses the acceleration of the human body to enable a key-handshake between two devices placed on a body.

The objective is to demonstrate the idea presented in my research paper: [Wearable security: Key derivation for Body Area sensor Networks based on host movement](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7745050) + [IEEE Link](https://ieeexplore.ieee.org/document/7745050/). 

This idea was refined and further tested on in my Final Year Project at the City University of Hong Kong before my transfer which led to the development of this application. My [project report](https://drive.google.com/file/d/16q2KzqjNrt2qmz5voeV2zlas-EH6liP5/view?usp=sharing) goes into further details regarding the app and scheme that delves beyond the README and this was my [consolidated presentation](https://drive.google.com/file/d/1liRAGPLuUvo6tPHBB1fEZC24soPc6iIi/view?usp=sharing) for people who prefer pictures.

The scheme currently only supports similar motion anywhere on the human body, which is primarily the human torso. A future consideration for research would be to extend the scheme such that it supports devices on different parts of the human body regardless of local positioning.

Disclaimer: App is a little ugly as it was developed only as a Proof-of-Concept for my project.

## Supported devices
The following devices work accurately with the scheme as testing was done with their accelerometer sampling rates:

  * One Plus One
  * Nexus 6P (recommended)
  
Although in theory, the scheme should work with any devices with the same sampling rates - I just find it to be a good habit to not tread and get over-zealous beyond the realm of certainty. :-)

## Installation

Install the APK in root directory on two Android devices of the same kind.

## Running the app
This is just to describe how one can demo a successful handshake using this app.

### Process flow

 1. Pair both devices via bluetooth from the dotted menu.
 2. Attach both devices anywhere on your torso.
 3. Press the start button on either device.
 4. Wait 5 seconds.
 5. Walk straight for 15 seconds.
 6. Derive and generate the key on one device.
 7. The other device will show a popup displaying whether the key-generation was successful.
 
### Screenshots

Connecting action:

<img src="https://user-images.githubusercontent.com/6462536/45339046-92d84380-b55e-11e8-836b-9fd34d4307f0.png" width="250">

Connected device:

<img src="https://user-images.githubusercontent.com/6462536/45339039-8227cd80-b55e-11e8-9b74-b81b177d1844.png" width="250">
