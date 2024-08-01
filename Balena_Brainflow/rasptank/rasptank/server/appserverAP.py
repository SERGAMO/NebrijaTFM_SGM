#!/usr/bin/env/python
# File name   : appserverAP.py
# Author      : William
# Date        : 2019/10/28

import socket
import threading
import time
import os
from rpi_ws281x import *
import robotLight
import LED
import move
import servo
import RPIservo

SERVO_SENSORS = 11
SERVO_SHOULDER = 12
SERVO_ELBOW = 13
SERVO_WRIST = 14
SERVO_GRIPPER = 15

#LED  = LED.LED()
#LED.colorWipe(Color(80,255,0))
#LED.colorWipe(80,255,0)

RL=robotLight.RobotLight()
RL.start()
RL.breath(70,70,255)


##################################################################
functionMode = 0
speed_set = 100
rad = 0.5
turnWiggle = 60


scGear = RPIservo.ServoCtrl()
scGear.moveInit()

#Servo 11
Sensors_sc = RPIservo.ServoCtrl()
Sensors_sc.start()

#Servo 12
Shoulder_sc = RPIservo.ServoCtrl()
Shoulder_sc.start()

#Servo 13
Elbow_sc = RPIservo.ServoCtrl()
Elbow_sc.start()

#Servo 14
Wrist_sc = RPIservo.ServoCtrl()
Wrist_sc.start()

#Servo 15
Gripper_sc = RPIservo.ServoCtrl()
Gripper_sc.start()

# modeSelect = 'none'
modeSelect = 'PT'

init_pwmScGear = scGear.initPos[0]
init_pwmScSensors = scGear.initPos[1]
init_pwmScWrist = scGear.initPos[2]
init_pwmScShoulder = scGear.initPos[3]
init_pwmScElbow = scGear.initPos[4]
init_pwmScGripper = scGear.initPos[5]
##################################################################



step_set = 1
speed_set = 100
rad = 0.6

direction_command = 'no'
turn_command = 'no'
pos_input = 1
catch_input = 1
cir_input = 6

##################################################################
def servoPosInit():
    print("Init Servos")
    scGear.initConfig(0, init_pwmScGear, 1)
    Sensors_sc.initConfig(1, init_pwmScSensors, 1)
    Wrist_sc.initConfig(2, init_pwmScWrist, 1)
    Shoulder_sc.initConfig(3, init_pwmScShoulder, 1)
    Elbow_sc.initConfig(4, init_pwmScElbow, 1)
    Gripper_sc.initConfig(5, init_pwmScGripper, 1)
###################################################################

def app_ctrl():
    app_HOST = ''
    app_PORT = 10123
    app_BUFSIZ = 1024
    app_ADDR = (app_HOST, app_PORT)

    def  ap_thread():
        os.system("sudo create_ap wlan0 eth0 AdeeptCar 12345678")

    def setup():
        move.setup()
        Sensors_sc.moveServoInit([SERVO_SENSORS])
        Shoulder_sc.moveServoInit([SERVO_SHOULDER])
        Elbow_sc.moveServoInit([SERVO_ELBOW])
        Wrist_sc.moveServoInit([SERVO_WRIST])            
        Gripper_sc.moveServoInit([SERVO_GRIPPER])        
        servoPosInit()
        # try:
        #     RL=robotLight.RobotLight()
        #     RL.start()
        #     RL.breath(70,70,255)
        # except ModuleNotFoundError as e:
        #     print('Use "sudo pip3 install rpi_ws281x" to install WS_281x package\n使用"sudo pip3 install rpi_ws281x"命令来安装rpi_ws281x')
        #     pass



    def appCommand(data_input):
        print('Command RCV: ' + data_input)
        global direction_command, turn_command, pos_input, catch_input, cir_input
        if data_input == 'forwardStart\n':
            direction_command = 'forward'
            move.move(speed_set, direction_command, turn_command, rad)

        elif data_input == 'backwardStart\n':
            direction_command = 'backward'
            move.move(speed_set, direction_command, turn_command, rad)

        elif data_input == 'leftStart\n':
            turn_command = 'left'
            move.move(speed_set, direction_command, turn_command, rad)

        elif data_input == 'rightStart\n':
            turn_command = 'right'
            move.move(speed_set, direction_command, turn_command, rad)

        elif 'forwardStop' in data_input:
            direction_command = 'no'
            move.move(speed_set, direction_command, turn_command, rad)

        elif 'backwardStop' in data_input:
            direction_command = 'no'
            move.move(speed_set, direction_command, turn_command, rad)

        elif 'leftStop' in data_input:
            turn_command = 'no'
            move.move(speed_set, direction_command, turn_command, rad)

        elif 'rightStop' in data_input:
            turn_command = 'no'
            move.move(speed_set, direction_command, turn_command, rad)
            pass


        if data_input == 'lookLeftStart\n':
            if cir_input < 12:
                cir_input+=1
            #servo.cir_pos(cir_input)
            Wrist_sc.singleServo(SERVO_WRIST, -1, 3)
            init_pwm1 = Wrist_sc.lastPos[1]
            Wrist_sc.initConfig(1,Wrist_sc.lastPos[1],1)
            #replace_num('init_pwm1 = ', Sensors_sc.lastPos[1])            
            

        elif data_input == 'lookRightStart\n': 
            if cir_input > 1:
                cir_input-=1
            #servo.cir_pos(cir_input)
            Wrist_sc.singleServo(SERVO_WRIST, 1, 3)
            init_pwm1 = Wrist_sc.lastPos[1]
            Wrist_sc.initConfig(1,Wrist_sc.lastPos[1],1)
            

        elif data_input == 'downStart\n':
            #servo.camera_ang('lookdown',10)
            Sensors_sc.singleServo(SERVO_SENSORS, 1, 3)
            Shoulder_sc.singleServo(SERVO_SHOULDER, 1, 3)
            init_pwm2 = Sensors_sc.lastPos[2]
            init_pwm3 = Shoulder_sc.lastPos[3]
            Sensors_sc.initConfig(2,Sensors_sc.lastPos[2],1)
            Shoulder_sc.initConfig(3,Shoulder_sc.lastPos[3],1)

            #print('LLLLLS',Wrist_sc.lastPos[2])

        elif data_input == 'upStart\n':
            #servo.camera_ang('lookup',10)
            Sensors_sc.singleServo(SERVO_SENSORS, -1, 3)
            Shoulder_sc.singleServo(SERVO_SHOULDER, -1, 3)
            init_pwm2 = Sensors_sc.lastPos[2]
            init_pwm3 = Shoulder_sc.lastPos[3]
            Sensors_sc.initConfig(2,Sensors_sc.lastPos[2],1)
            Shoulder_sc.initConfig(3,Shoulder_sc.lastPos[3],1)
            #print('LLLLLS',Wrist_sc.lastPos[2])
            

        elif 'lookLeftStop' in data_input:
            Wrist_sc.stopWiggle()
            #pass
        elif 'lookRightStop' in data_input:
            Wrist_sc.stopWiggle()
            #pass
        elif 'downStop' in data_input:
            Sensors_sc.stopWiggle()
            Shoulder_sc.stopWiggle()
            #pass
        elif 'upStop' in data_input:
            Sensors_sc.stopWiggle()
            Shoulder_sc.stopWiggle()
            #pass

        elif 'grab' == data_input:
            Gripper_sc.singleServo(SERVO_GRIPPER, 1, 3)

        elif 'loose' == data_input:
            Gripper_sc.singleServo(SERVO_GRIPPER, -1, 3)

        elif 'stop' == data_input:
            Gripper_sc.stopWiggle()

        elif 'home' == data_input:
            Sensors_sc.moveServoInit([11])                    
            Shoulder_sc.moveServoInit([12])
            Elbow_sc.moveServoInit([13])        
            Wrist_sc.moveServoInit([14])
            Gripper_sc.moveServoInit([15])
            


#************* APP Commands****************
        if data_input == 'aStart\n':
            if pos_input < 17:
                pos_input+=1
            #servo.hand_pos(pos_input)
            Elbow_sc.singleServo(SERVO_ELBOW, -1, 7)
            #pass

        elif data_input == 'bStart\n':
            if pos_input > 1:
                pos_input-=1
            #servo.hand_pos(pos_input)
            Elbow_sc.singleServo(SERVO_ELBOW, 1, 7)
            #pass

        elif data_input == 'cStart\n':
            if catch_input < 13:
                catch_input+=3
            #servo.catch(catch_input)
            Gripper_sc.singleServo(SERVO_GRIPPER, 1, 3)
            pass

        elif data_input == 'dStart\n':
            if catch_input > 1:
                catch_input-=3
            #servo.catch(catch_input)
            Gripper_sc.singleServo(SERVO_GRIPPER, -1, 3)
            pass

        elif 'aStop' in data_input:
            Elbow_sc.stopWiggle()
            #pass

        elif 'bStop' in data_input:
            Elbow_sc.stopWiggle()
            #pass

        elif 'cStop' in data_input:
            Gripper_sc.stopWiggle()
            #pass

        elif 'dStop' in data_input:
            Gripper_sc.stopWiggle()
            pass

        print(data_input)


    def appconnect():
        global AppCliSock, AppAddr
        try:
            s =socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
            s.connect(("1.1.1.1",80))
            ipaddr_check=s.getsockname()[0]
            s.close()
            print(ipaddr_check)

            AppSerSock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            AppSerSock.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
            AppSerSock.bind(app_ADDR)
            AppSerSock.listen(5)
            print('waiting for App connection...')
            AppCliSock, AppAddr = AppSerSock.accept()
            print('...App connected from :', AppAddr)
        except:
            ap_threading=threading.Thread(target=ap_thread)   #Define a thread for data receiving
            ap_threading.daemon = True                          #'True' means it is a front thread,it would close when the mainloop() closes
            ap_threading.start()                                  #Thread starts

            #LED.colorWipe(Color(0,16,50))
            RL.setColor(0,16,50)            
            time.sleep(1)
            #LED.colorWipe(Color(0,16,100))
            RL.setColor(0,16,100)
            time.sleep(1)
            #LED.colorWipe(Color(0,16,150))
            RL.setColor(0,16,150)
            time.sleep(1)
            #LED.colorWipe(Color(0,16,200))
            RL.setColor(0,16,200)
            time.sleep(1)
            #LED.colorWipe(Color(0,16,255))
            RL.setColor(0,16,255)
            time.sleep(1)
            #LED.colorWipe(Color(35,255,35))
            RL.setColor(35,255,35)

            AppSerSock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            AppSerSock.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
            AppSerSock.bind(app_ADDR)
            AppSerSock.listen(5)
            print('waiting for App connection...')
            AppCliSock, AppAddr = AppSerSock.accept()
            print('...App connected from :', AppAddr)

    appconnect()
    setup()
    app_threading=threading.Thread(target=appconnect)         #Define a thread for FPV and OpenCV
    app_threading.daemon = True                            #'True' means it is a front thread,it would close when the mainloop() closes
    app_threading.start()                                     #Thread starts

    while 1:
        data = ''
        data = str(AppCliSock.recv(app_BUFSIZ).decode())
        if not data:
            continue
        appCommand(data)
        pass

AppConntect_threading=threading.Thread(target=app_ctrl)         #Define a thread for FPV and OpenCV
AppConntect_threading.daemon = True                             #'True' means it is a front thread,it would close when the mainloop() closes
AppConntect_threading.start()                                     #Thread starts

if __name__ == '__main__':
    i = 1
    while 1:
        i += 1
        print(i)
        time.sleep(30)
        pass