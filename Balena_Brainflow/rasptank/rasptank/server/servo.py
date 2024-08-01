#!/usr/bin/env python3
# File name   : servo.py
# Description : Control Servos
# Author      : William
# Date        : 2019/02/23
from __future__ import division
import time
import RPi.GPIO as GPIO
from board import SCL, SDA
import busio
import digitalio
import sys
#import Adafruit_PCA9685
# Import the PCA9685 module.
from adafruit_pca9685 import PCA9685
from adafruit_motor import servo

import ultra


# Create the I2C bus interface.
i2c_bus = busio.I2C(SCL, SDA)
# Create a simple PCA9685 class instance.
pwm = PCA9685(i2c_bus)
pwm.frequency = 50


#Servo Channels. Board has 16 channels (0-15)
#servo0 = servo.Servo(pwm.channels[0], min_pulse=100, max_pulse=560)
servo0 = servo.Servo(pwm.channels[0], actuation_range=180)
servo1 = servo.Servo(pwm.channels[1], actuation_range=180)
servo2 = servo.Servo(pwm.channels[2], actuation_range=180)
servo3 = servo.Servo(pwm.channels[3], actuation_range=180)
servo4 = servo.Servo(pwm.channels[4], actuation_range=180)
servo5 = servo.Servo(pwm.channels[5], actuation_range=180)
servo6 = servo.Servo(pwm.channels[6], actuation_range=180)
servo7 = servo.Servo(pwm.channels[7], actuation_range=180)
servo8 = servo.Servo(pwm.channels[8], actuation_range=180)
servo9 = servo.Servo(pwm.channels[9], actuation_range=180)
servo10 = servo.Servo(pwm.channels[10], actuation_range=180)
servo11 = servo.Servo(pwm.channels[11], actuation_range=180)
servo12 = servo.Servo(pwm.channels[12], actuation_range=180)
servo13 = servo.Servo(pwm.channels[13], actuation_range=180)
servo14 = servo.Servo(pwm.channels[14], actuation_range=180)
servo15 = servo.Servo(pwm.channels[15], actuation_range=180)

'''
change this form 1 to 0 to reverse servos
'''
pwm0_direction = 1
pwm1_direction = 1
pwm2_direction = 1
pwm3_direction = 1


#pwm = Adafruit_PCA9685.PCA9685()
#pwm.set_pwm_freq(50)



pwm0_init = 300
pwm0_max  = 450
pwm0_min  = 150
pwm0_pos  = pwm0_init

pwm1_init = 300
pwm1_max  = 480
pwm1_min  = 160
pwm1_pos  = pwm1_init

pwm2_init = 300
pwm2_max  = 500
pwm2_min  = 100
pwm2_pos  = pwm2_init

pwm3_init = 300
pwm3_max  = 500
pwm3_min  = 300
pwm3_pos  = pwm3_init

org_pos = 300


def radar_scan():
	global pwm0_pos
	scan_result = 'U: '
	scan_speed = 1
	if pwm0_direction:
		pwm0_pos = pwm0_max
		servo0.angle = angle_from_pwm(pwm0_pos)		
		#pwm.set_pwm(0, 0, pwm0_pos)
		time.sleep(0.5)
		scan_result += str(ultra.checkdist())
		scan_result += ' '
		while pwm0_pos>pwm0_min:
			pwm0_pos-=scan_speed
			#pwm.set_pwm(0, 0, pwm0_pos)
			servo0.angle = angle_from_pwm(pwm0_pos)	
			scan_result += str(ultra.checkdist())
			scan_result += ' '
		#pwm.set_pwm(0, 0, pwm0_init)
		servo0.angle = angle_from_pwm(pwm0_init)	
	else:
		pwm0_pos = pwm0_min
		#pwm.set_pwm(0, 0, pwm0_pos)
		servo0.angle = angle_from_pwm(pwm0_pos)
		time.sleep(0.5)
		scan_result += str(ultra.checkdist())
		scan_result += ' '
		while pwm0_pos<pwm0_max:
			pwm0_pos+=scan_speed
			#pwm.set_pwm(0, 0, pwm0_pos)
			servo0.angle = angle_from_pwm(pwm0_pos)	
			scan_result += str(ultra.checkdist())
			scan_result += ' '
		#pwm.set_pwm(0, 0, pwm0_init)
		servo0.angle = angle_from_pwm(pwm0_init)	
	return scan_result



def angle_from_pwm(pwm_in):
	angle = (pwm_in - 100)/2.55
	if angle > 180:
		angle = 180
	if angle < 0:
		angle = 0	
	
	return angle

def ctrl_range(raw, max_genout, min_genout):
	if raw > max_genout:
		raw_output = max_genout
	elif raw < min_genout:
		raw_output = min_genout
	else:
		raw_output = raw
	return int(raw_output)


def camera_ang(direction, ang):
	global org_pos
	if ang == 'no':
		ang = 50
	if look_direction:
		if direction == 'lookdown':
			org_pos+=ang
			org_pos = ctrl_range(org_pos, look_max, look_min)
		elif direction == 'lookup':
			org_pos-=ang
			org_pos = ctrl_range(org_pos, look_max, look_min)
		elif direction == 'home':
			org_pos = 300
	else:
		if direction == 'lookdown':
			org_pos-=ang
			org_pos = ctrl_range(org_pos, look_max, look_min)
		elif direction == 'lookup':
			org_pos+=ang
			org_pos = ctrl_range(org_pos, look_max, look_min)
		elif direction == 'home':
			org_pos = 300	

	#pwm.set_all_pwm(0,org_pos)
	for i in range(0,16):
		pwm.channels[i].duty_cycle = org_pos


def lookleft(speed):
	global pwm0_pos
	if pwm0_direction:
		pwm0_pos += speed
		pwm0_pos = ctrl_range(pwm0_pos, pwm0_max, pwm0_min)
		#pwm.set_pwm(0, 0, pwm0_pos)
		servo0.angle = angle_from_pwm(pwm0_pos)	
	else:
		pwm0_pos -= speed
		pwm0_pos = ctrl_range(pwm0_pos, pwm0_max, pwm0_min)
		#pwm.set_pwm(0, 0, pwm0_pos)
		servo0.angle = angle_from_pwm(pwm0_pos)	


def lookright(speed):
	global pwm0_pos
	if pwm0_direction:
		pwm0_pos -= speed
		pwm0_pos = ctrl_range(pwm0_pos, pwm0_max, pwm0_min)
		#pwm.set_pwm(0, 0, pwm0_pos)
		servo0.angle = angle_from_pwm(pwm0_pos)	
	else:
		pwm0_pos += speed
		pwm0_pos = ctrl_range(pwm0_pos, pwm0_max, pwm0_min)
		#pwm.set_pwm(0, 0, pwm0_pos)
		servo0.angle = angle_from_pwm(pwm0_pos)	


def up(speed):
	global pwm1_pos
	if pwm1_direction:
		pwm1_pos -= speed
		pwm1_pos = ctrl_range(pwm1_pos, pwm1_max, pwm1_min)
		#pwm.set_pwm(1, 0, pwm1_pos)
		servo1.angle = angle_from_pwm(pwm1_pos)	
	else:
		pwm1_pos += speed
		pwm1_pos = ctrl_range(pwm1_pos, pwm1_max, pwm1_min)
		#pwm.set_pwm(1, 0, pwm1_pos)
		servo1.angle = angle_from_pwm(pwm1_pos)	
	#print(pwm1_pos)


def down(speed):
	global pwm1_pos
	if pwm1_direction:
		pwm1_pos += speed
		pwm1_pos = ctrl_range(pwm1_pos, pwm1_max, pwm1_min)
		#pwm.set_pwm(1, 0, pwm1_pos)
		servo1.angle = angle_from_pwm(pwm1_pos)	
	else:
		pwm1_pos -= speed
		pwm1_pos = ctrl_range(pwm1_pos, pwm1_max, pwm1_min)
		#pwm.set_pwm(1, 0, pwm1_pos)
		servo1.angle = angle_from_pwm(pwm1_pos)	
	#print(pwm1_pos)

def lookup(speed):
	global pwm2_pos
	if pwm2_direction:
		pwm2_pos -= speed
		pwm2_pos = ctrl_range(pwm2_pos, pwm2_max, pwm2_min)
		#pwm.set_pwm(2, 0, pwm2_pos)
		servo2.angle = angle_from_pwm(pwm2_pos)	
	else:
		pwm2_pos += speed
		pwm2_pos = ctrl_range(pwm2_pos, pwm2_max, pwm2_min)
		#pwm.set_pwm(2, 0, pwm2_pos)
		servo2.angle = angle_from_pwm(pwm2_pos)	

def lookdown(speed):
	global pwm2_pos
	if pwm2_direction:
		pwm2_pos += speed
		pwm2_pos = ctrl_range(pwm2_pos, pwm2_max, pwm2_min)
		#pwm.set_pwm(2, 0, pwm2_pos)
		servo2.angle = angle_from_pwm(pwm2_pos)	
	else:
		pwm2_pos -= speed
		pwm2_pos = ctrl_range(pwm2_pos, pwm2_max, pwm2_min)
		#pwm.set_pwm(2, 0, pwm2_pos)
		servo2.angle = angle_from_pwm(pwm2_pos)	


def grab(speed):
	global pwm3_pos
	if pwm3_direction:
		pwm3_pos -= speed
		pwm3_pos = ctrl_range(pwm3_pos, pwm3_max, pwm3_min)
		#pwm.set_pwm(3, 0, pwm3_pos)
		servo3.angle = angle_from_pwm(pwm3_pos)	
	else:
		pwm3_pos += speed
		pwm3_pos = ctrl_range(pwm3_pos, pwm3_max, pwm3_min)
		#pwm.set_pwm(3, 0, pwm3_pos)
		servo3.angle = angle_from_pwm(pwm3_pos)	
	print(pwm3_pos)


def loose(speed):
	global pwm3_pos
	if pwm3_direction:
		pwm3_pos += speed
		pwm3_pos = ctrl_range(pwm3_pos, pwm3_max, pwm3_min)
		#pwm.set_pwm(3, 0, pwm3_pos)
		servo3.angle = angle_from_pwm(pwm3_pos)	
	else:
		pwm3_pos -= speed
		pwm3_pos = ctrl_range(pwm3_pos, pwm3_max, pwm3_min)
		#pwm.set_pwm(3, 0, pwm3_pos)
		servo3.angle = angle_from_pwm(pwm3_pos)	
	print(pwm3_pos)


def servo_init():
	#pwm.set_pwm(0, 0, pwm0_pos)
	#pwm.set_pwm(1, 0, pwm1_pos)
	#pwm.set_pwm(2, 0, pwm2_max)
	#pwm.set_pwm(3, 0, pwm3_pos)
	servo0.angle = angle_from_pwm(pwm0_pos)	
	servo1.angle = angle_from_pwm(pwm1_pos)	
	servo2.angle = angle_from_pwm(pwm2_pos)	
	servo3.angle = angle_from_pwm(pwm3_pos)	
	try:
		#pwm.set_all_pwm(0, 300)
		#for i in range(0,16):
		servo0.angle = angle_from_pwm(300)	
		servo1.angle = angle_from_pwm(300)	
		servo2.angle = angle_from_pwm(300)	
		servo3.angle = angle_from_pwm(300)	
		
	except:
		pass


def clean_all():
	global pwm
	#pwm = Adafruit_PCA9685.PCA9685()
	#pwm.set_pwm_freq(50)
	pwm = PCA9685(i2c_bus)
	pwm.frequency = 50	
	#pwm.set_all_pwm(0, 0)
	#for i in range(0,16):
	servo0.angle = angle_from_pwm(0)	
	servo1.angle = angle_from_pwm(0)	
	servo2.angle = angle_from_pwm(0)	
	servo3.angle = angle_from_pwm(0)	


def ahead():
	global pwm0_pos, pwm1_pos
	#pwm.set_pwm(0, 0, pwm0_init)
	servo0.angle = angle_from_pwm(pwm0_init)	
	#pwm.set_pwm(1, 0, (pwm1_max-20))
	servo1.angle = angle_from_pwm(pwm1_max - 20)		
	pwm0_pos = pwm0_init
	pwm1_pos = pwm1_max-20


def get_direction():
	return (pwm0_pos - pwm0_init)


if __name__ == '__main__':
	
	channel = 0	# servo port number.
	while True:
		#pwm.set_pwm(channel, 0, 150)
		servo0.angle = angle_from_pwm(150)	
		time.sleep(1)
		#pwm.set_pwm(channel, 0, 450)
		servo0.angle = angle_from_pwm(450)	
		time.sleep(1)

