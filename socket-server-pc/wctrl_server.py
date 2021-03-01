#!/usr/bin/python3
#modprobe -i uinput
import socket, re, uinput

ipaddr = '192.168.1.69'
port = 1234

jmin = -127
jmax = 127

rmin = -1.0
rmax = 1.0

jstk_rst = 0.01

events = (
	uinput.BTN_START,						#Start
	uinput.BTN_SELECT,						#Select
	uinput.ABS_X + (jmin, jmax, 0, 0),		#Right Joystick X
	uinput.ABS_Y + (jmin, jmax, 0, 0),		#Right Joystick Y
	uinput.BTN_THUMBL,						#Left Joystick Button
	uinput.ABS_Z + (jmin, jmax, 0, 0),		#Left Joystick X
	uinput.ABS_RZ + (jmin, jmax, 0, 0),		#Left Joystick Y
	uinput.BTN_THUMBR,						#Right Joystick Button
	uinput.BTN_X,							#X
	uinput.BTN_Y,							#Y
	uinput.BTN_A,							#A
	uinput.BTN_B,							#B
	uinput.BTN_TR,							#R1
	uinput.BTN_TR2,							#R2
	uinput.BTN_TL,							#L1
	uinput.BTN_TL2							#L2
)

button_mapping = {
	"197": events[0],
	"196": events[1],
	"198": events[4],
	"199": events[7],
	"190": events[8],
	"191": events[9],
	"188": events[10],
	"189": events[11],
	"192": events[14],
	"194": events[15],
	"193": events[12],
	"195": events[13],
}

device = uinput.Device(events)

def map_value(s, a1, a2, b1, b2):
	return b1 + (s - a1) * (b2 - b1) / (a2 - a1)

def process_joystick(jstk, coord, pos):
	value = float(pos)
	if value < jstk_rst and value > (-jstk_rst):
		value = 0
	else:
		value = int(map_value(float(pos), rmin, rmax, jmin, jmax))
	if jstk == 'right':
		if coord == 'x':
			device.emit(uinput.ABS_Z, value)
		else:
			device.emit(uinput.ABS_RZ, value)
	else:
		if coord == 'x':
			device.emit(uinput.ABS_X, value)
		else:
			device.emit(uinput.ABS_Y, value)

def process_key(key, action):
	if action == "up":
		device.emit(button_mapping[key], 0)
	else:
		a = button_mapping[key]
		if a == uinput.BTN_THUMBL:
			device.emit(uinput.ABS_X, 0)
			device.emit(uinput.ABS_Y, 0)
		elif a == uinput.BTN_THUMBR:
			device.emit(uinput.ABS_Z, 0)
			device.emit(uinput.ABS_RZ, 0)
		device.emit(a, 1)

def process_data(data):
	#if is more than one instruction
	data = data.split('\n')
	for i in data:
		#know if it is a KEY instruction or Joystick
		rcmd = i
		jstk = re.findall("right|left", rcmd)
		try:
			if(len(jstk) > 0):
				jstk = jstk[0]
				coord = re.findall("x|y", rcmd)[0]
				pos = re.findall("[-]?\d+.\d+", rcmd)[0]
				#process event
				#print("Joystick: {0}\tCoord: {1}\tPosition: {2}".format(jstk, coord, pos))
				process_joystick(jstk, coord, pos)
			else:
				key = re.findall("\d+", rcmd)
				if(len(key) > 0):
					key = key[0]
					action = re.findall("up|down", rcmd)[0]
					#process event
					#print("Key: {0}\tAction: {1}".format(key, action))
					process_key(key, action)
		except:
			print("Error!")

# Create a TCP/IP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

server_address = (ipaddr, port)
print('Wireless Control Server starting up on {} port {}'.format(*server_address))
sock.bind(server_address)

sock.listen(1)

while True:
	print('waiting for a connection')
	connection, client_address = sock.accept()
	try:
		print('connection from', client_address)
		while True:
			data = connection.recv(255).decode()
			process_data(data)

	finally:
		print("Closing the connection...")
		connection.close()
		device.destroy()
