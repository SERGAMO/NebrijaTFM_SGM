#!/usr/bin/env python3
# File name   : servo.py
# Description : Control Servos
# Author	  : William
# Date		: 2019/02/23
from __future__ import division
import time
import RPi.GPIO as GPIO
from board import SCL, SDA
import busio
import sys
#import Adafruit_PCA9685
from adafruit_pca9685 import PCA9685
from adafruit_motor import servo

import threading

import random
'''
change this form 1 to -1 to reverse servos
'''
#pwm = Adafruit_PCA9685.PCA9685()
#pwm.set_pwm_freq(50)

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

servo_list = [servo0, servo1, servo2, servo3, servo4, servo5, servo6, servo7, servo8, servo9, servo10, servo11, servo12, servo13, servo14, servo15]


init_pwm0 = 300
init_pwm1 = 300
init_pwm2 = 300
init_pwm3 = 300

init_pwm4 = 300
init_pwm5 = 300
init_pwm6 = 300
init_pwm7 = 300

init_pwm8 = 300
init_pwm9 = 300
init_pwm10 = 300
init_pwm11 = 300

init_pwm12 = 300
init_pwm13 = 300
init_pwm14 = 300
init_pwm15 = 300

class ServoCtrl(threading.Thread):

	def __init__(self, *args, **kwargs):
		self.sc_direction = [1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1]
		self.initPos = [init_pwm0,init_pwm1,init_pwm2,init_pwm3,
						init_pwm4,init_pwm5,init_pwm6,init_pwm7,
						init_pwm8,init_pwm9,init_pwm10,init_pwm11,
						init_pwm12,init_pwm13,init_pwm14,init_pwm15]
		self.goalPos = [300,300,300,300, 300,300,300,300 ,300,300,300,300 ,300,300,300,300]
		self.nowPos  = [300,300,300,300, 300,300,300,300 ,300,300,300,300 ,300,300,300,300]
		self.bufferPos  = [300.0,300.0,300.0,300.0, 300.0,300.0,300.0,300.0 ,300.0,300.0,300.0,300.0 ,300.0,300.0,300.0,300.0]
		self.lastPos = [300,300,300,300, 300,300,300,300 ,300,300,300,300 ,300,300,300,300]
		self.ingGoal = [300,300,300,300, 300,300,300,300 ,300,300,300,300 ,300,300,300,300]
		# self.maxPos  = [560,560,560,560, 560,560,560,560 ,560,560,560,560 ,560,560,560,560]
		self.maxPos  = [520,520,520,520, 520,520,520,520 ,520,520,520,520 ,520,520,520,520]
		self.minPos  = [100,100,100,100, 100,100,100,100 ,100,100,100,100 ,100,100,100,100]
		self.scSpeed = [0,0,0,0, 0,0,0,0 ,0,0,0,0 ,0,0,0,0]

		# self.ctrlRangeMax = 560
		self.ctrlRangeMax = 520
		self.ctrlRangeMin = 100
		self.angleRange = 180

		'''
		scMode: 'init' 'auto' 'certain' 'quick' 'wiggle'
		'''
		self.scMode = 'auto'
		self.scTime = 2.0
		self.scSteps = 30
		
		self.scDelay = 0.037
		self.scMoveTime = 0.037

		self.goalUpdate = 0
		self.wiggleID = 0
		self.wiggleDirection = 1

		super(ServoCtrl, self).__init__(*args, **kwargs)
		self.__flag = threading.Event()
		self.__flag.clear()


	def pause(self):
		print('......................pause..........................')
		self.__flag.clear()


	def resume(self):
		print('resume')
		self.__flag.set()

#https://github.com/adafruit/Adafruit_CircuitPython_PCA9685/blob/main/examples/pca9685_servo.py
	def moveInit(self):
		self.scMode = 'init'
		for i in range(0,16):
			#pwm.set_pwm(channel, 0, pulse)
			#pwm.set_pwm(i,0,self.initPos[i])
			servo_list[i].angle = self.angle_from_pwm(self.initPos[i])
			#servo12.angle = self.initPos[i]
			self.lastPos[i] = self.initPos[i]
			self.nowPos[i] = self.initPos[i]
			self.bufferPos[i] = float(self.initPos[i])
			self.goalPos[i] = self.initPos[i]
		self.pause()


	def initConfig(self, ID, initInput, moveTo):
		if initInput > self.minPos[ID] and initInput < self.maxPos[ID]:
			self.initPos[ID] = initInput
			if moveTo:
				#pwm.set_pwm(ID,0,self.initPos[ID])
				servo_list[ID].angle = self.angle_from_pwm(self.initPos[ID])
		else:
			print('initPos Value Error.')


	def moveServoInit(self, ID):
		self.scMode = 'init'
		for i in range(0,len(ID)):
			#pwm.set_pwm(ID[i], 0, self.initPos[ID[i]])
			servo_list[i].angle = self.angle_from_pwm(self.initPos[i])
			self.lastPos[ID[i]] = self.initPos[ID[i]]
			self.nowPos[ID[i]] = self.initPos[ID[i]]
			self.bufferPos[ID[i]] = float(self.initPos[ID[i]])
			self.goalPos[ID[i]] = self.initPos[ID[i]]
		self.pause()


	def posUpdate(self):
		self.goalUpdate = 1
		for i in range(0,16):
			self.lastPos[i] = self.nowPos[i]
		self.goalUpdate = 0


	def speedUpdate(self, IDinput, speedInput):
		for i in range(0,len(IDinput)):
			self.scSpeed[IDinput[i]] = speedInput[i]


	def moveAuto(self):
		for i in range(0,16):
			self.ingGoal[i] = self.goalPos[i]

		for i in range(0, self.scSteps):
			for dc in range(0,16):
				if not self.goalUpdate:
					self.nowPos[dc] = int(round((self.lastPos[dc] + (((self.goalPos[dc] - self.lastPos[dc])/self.scSteps)*(i+1))),0))
					#pwm.set_pwm(dc, 0, self.nowPos[dc])
					servo_list[dc].angle = self.angle_from_pwm(self.nowPos[dc])

				if self.ingGoal != self.goalPos:
					self.posUpdate()
					time.sleep(self.scTime/self.scSteps)
					return 1
			time.sleep((self.scTime/self.scSteps - self.scMoveTime))

		self.posUpdate()
		self.pause()
		return 0


	def moveCert(self):
		for i in range(0,16):
			self.ingGoal[i] = self.goalPos[i]
			self.bufferPos[i] = self.lastPos[i]

		while self.nowPos != self.goalPos:
			for i in range(0,16):
				if self.lastPos[i] < self.goalPos[i]:
					self.bufferPos[i] += self.pwmGenOut(self.scSpeed[i])/(1/self.scDelay)
					newNow = int(round(self.bufferPos[i], 0))
					if newNow > self.goalPos[i]:newNow = self.goalPos[i]
					self.nowPos[i] = newNow
				elif self.lastPos[i] > self.goalPos[i]:
					self.bufferPos[i] -= self.pwmGenOut(self.scSpeed[i])/(1/self.scDelay)
					newNow = int(round(self.bufferPos[i], 0))
					if newNow < self.goalPos[i]:newNow = self.goalPos[i]
					self.nowPos[i] = newNow

				if not self.goalUpdate:
					#pwm.set_pwm(i, 0, self.nowPos[i])
					servo_list[i].angle = self.angle_from_pwm(self.nowPos[i])

				if self.ingGoal != self.goalPos:
					self.posUpdate()
					return 1
			self.posUpdate()
			time.sleep(self.scDelay-self.scMoveTime)

		else:
			self.pause()
			return 0


	def pwmGenOut(self, angleInput):
		return int(round(((self.ctrlRangeMax-self.ctrlRangeMin)/self.angleRange*angleInput),0))


	def setAutoTime(self, autoSpeedSet):
		self.scTime = autoSpeedSet


	def setDelay(self, delaySet):
		self.scDelay = delaySet


	def autoSpeed(self, ID, angleInput):
		self.scMode = 'auto'
		self.goalUpdate = 1
		for i in range(0,len(ID)):
			newGoal = self.initPos[ID[i]] + self.pwmGenOut(angleInput[i])*self.sc_direction[ID[i]]
			if newGoal>self.maxPos[ID[i]]:newGoal=self.maxPos[ID[i]]
			elif newGoal<self.minPos[ID[i]]:newGoal=self.minPos[ID[i]]
			self.goalPos[ID[i]] = newGoal
		self.goalUpdate = 0
		self.resume()


	def certSpeed(self, ID, angleInput, speedSet):
		self.scMode = 'certain'
		self.goalUpdate = 1
		for i in range(0,len(ID)):
			newGoal = self.initPos[ID[i]] + self.pwmGenOut(angleInput[i])*self.sc_direction[ID[i]]
			if newGoal>self.maxPos[ID[i]]:newGoal=self.maxPos[ID[i]]
			elif newGoal<self.minPos[ID[i]]:newGoal=self.minPos[ID[i]]
			self.goalPos[ID[i]] = newGoal
		self.speedUpdate(ID, speedSet)
		self.goalUpdate = 0
		self.resume()


	def moveWiggle(self):
		self.bufferPos[self.wiggleID] += self.wiggleDirection*self.sc_direction[self.wiggleID]*self.pwmGenOut(self.scSpeed[self.wiggleID])/(1/self.scDelay)
		newNow = int(round(self.bufferPos[self.wiggleID], 0))
		if self.bufferPos[self.wiggleID] > self.maxPos[self.wiggleID]:self.bufferPos[self.wiggleID] = self.maxPos[self.wiggleID]
		elif self.bufferPos[self.wiggleID] < self.minPos[self.wiggleID]:self.bufferPos[self.wiggleID] = self.minPos[self.wiggleID]
		self.nowPos[self.wiggleID] = newNow
		self.lastPos[self.wiggleID] = newNow
		if self.bufferPos[self.wiggleID] < self.maxPos[self.wiggleID] and self.bufferPos[self.wiggleID] > self.minPos[self.wiggleID]:
			#pwm.set_pwm(self.wiggleID, 0, self.nowPos[self.wiggleID])
			servo_list[self.wiggleID].angle = self.angle_from_pwm(self.nowPos[self.wiggleID])
		else:
			self.stopWiggle()
		time.sleep(self.scDelay-self.scMoveTime)


	def stopWiggle(self):
		self.pause()
		self.posUpdate()


	def singleServo(self, ID, direcInput, speedSet):
		self.wiggleID = ID
		self.wiggleDirection = direcInput
		self.scSpeed[ID] = speedSet
		self.scMode = 'wiggle'
		self.posUpdate()
		self.resume()


	def moveAngle(self, ID, angleInput):
		self.nowPos[ID] = int(self.initPos[ID] + self.sc_direction[ID]*self.pwmGenOut(angleInput))
		if self.nowPos[ID] > self.maxPos[ID]:self.nowPos[ID] = self.maxPos[ID]
		elif self.nowPos[ID] < self.minPos[ID]:self.nowPos[ID] = self.minPos[ID]
		self.lastPos[ID] = self.nowPos[ID]		
		#pwm.set_pwm(ID, 0, self.nowPos[ID])
		servo_list[ID].angle = self.angle_from_pwm(self.nowPos[ID])

		


	def scMove(self):
		if self.scMode == 'init':
			self.moveInit()
		elif self.scMode == 'auto':
			self.moveAuto()
		elif self.scMode == 'certain':
			self.moveCert()
		elif self.scMode == 'wiggle':
			self.moveWiggle()


	def angle_from_pwm(self, pwm_in):
		angle = (pwm_in - 100)/2.55
		if angle > 180:
			angle = 180
		if angle < 0:
			angle = 0			
		return angle
	
	def init_all_servos(self):
		servo0.angle = self.angle_from_pwm(self.initPos[0])	
		servo1.angle = self.angle_from_pwm(self.initPos[1])	
		servo2.angle = self.angle_from_pwm(self.initPos[2])	
		servo3.angle = self.angle_from_pwm(self.initPos[3])	
		servo4.angle = self.angle_from_pwm(self.initPos[4])	
		servo5.angle = self.angle_from_pwm(self.initPos[5])	
		servo6.angle = self.angle_from_pwm(self.initPos[6])	
		servo7.angle = self.angle_from_pwm(self.initPos[7])	
		servo8.angle = self.angle_from_pwm(self.initPos[8])	
		servo9.angle = self.angle_from_pwm(self.initPos[9])	
		servo10.angle = self.angle_from_pwm(self.initPos[10])	
		servo11.angle = self.angle_from_pwm(self.initPos[11])	
		servo12.angle = self.angle_from_pwm(self.initPos[12])	
		servo13.angle = self.angle_from_pwm(self.initPos[13])	
		servo14.angle = self.angle_from_pwm(self.initPos[14])	
		servo15.angle = self.angle_from_pwm(self.initPos[15])	


	def setPWM(self, ID, PWM_input):
		self.lastPos[ID] = PWM_input
		self.nowPos[ID] = PWM_input
		self.bufferPos[ID] = float(PWM_input)
		self.goalPos[ID] = PWM_input
		#pwm.set_pwm(ID, 0, PWM_input)
		servo_list[ID].angle = self.angle_from_pwm(self.nowPos[ID])
		self.pause()
	


	def run(self):
		while 1:
			self.__flag.wait()
			self.scMove()
			pass


if __name__ == '__main__':
	sc = ServoCtrl()
	sc.start()
	while 1:
		sc.moveAngle(0,(random.random()*100-50))
		time.sleep(1)
		sc.moveAngle(1,(random.random()*100-50))
		time.sleep(1)
		'''
		sc.singleServo(0, 1, 5)
		time.sleep(6)
		sc.singleServo(0, -1, 30)
		time.sleep(1)
		'''
		'''
		delaytime = 5
		sc.certSpeed([0,7], [60,0], [40,60])
		print('xx1xx')
		time.sleep(delaytime)

		sc.certSpeed([0,7], [0,60], [40,60])
		print('xx2xx')
		time.sleep(delaytime+2)

		# sc.moveServoInit([0])
		# time.sleep(delaytime)
		'''
		'''
		pwm.set_pwm(0,0,560)
		time.sleep(1)
		pwm.set_pwm(0,0,100)
		time.sleep(2)
		'''
		pass
	pass
