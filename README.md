# wired2less controller

Use your wired USB Game Controller as a wireless controller with your Android device

1. Load the apk into your Android device (socket-client-android)
2. connect a generic USB Game Controller
3. Install socket-server-pc dependencies:
```
pip3 install -r requirements.txt
```
4. Load uinput kernel module:
```
modprobe -i uinput
```
5. Change IP in socket-server-pc script
```
ipaddr = 'your.ip.address'
```
6. Start socket server
```
python3 wctrl_server.py
```
7. Connect to the socket server in Android device (default port: 1234):
```
your.ip.address:1234
```
8. Done, check if it works

## TO-DO:

Add Dpad controls functionality
