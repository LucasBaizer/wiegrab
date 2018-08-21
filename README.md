# Wiegrab
Wiegrab is an Android app to interact with a BLEKey.
Other apps commonly used with a BLEKey aren't specifically designed for the BLEKey.
Rather, you can use them for all BLE devices. This makes their UI hard to use and significantly more confusing.
Wiegrab is built from scratch to be used with a BLEKey, making the experience much more enjoyable.

# What's a BLEKey?
A [BLEKey](https://hackerwarehouse.com/product/blekey/) is a penetration testing tool used for manipulation of the Wiegand protocol.

# Features
* All the standard commands from the BLEKey command line are implemented
  * Replaying cards previously run through the network
  * Emulating custom cards
  * Reading cards previously run through the network
* Jamming the network, making the reader unusable
  * Achieved by emulating 0 "garbage" cards each second, like a DoS

# License
Wiegrab is under the [MIT License](LICENSE).
__Always make sure that you have permission from system/building administrators before using a BLEKey on their system.
I am not responsible for anything stupid you do, and neither is Wiegrab.__
