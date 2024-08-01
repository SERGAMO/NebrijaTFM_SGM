################################################################# 
#                       B L I N K   P O N G                     #
#################################################################

# Very simple Robot gripper control
# The objective is to try to open and close the gripper through blinks
# Needs Brainbit EEG band
#
# Usage: Blink to open gripper
#        Double blink to close gripper

# Based on:
# https://www.tensorflow.org/lite/performance/post_training_integer_quant
# https://www.tensorflow.org/lite/api_docs/java/org/tensorflow/lite/Tensor.QuantizationParams
#Example: https://gist.github.com/ShawnHymel/f7b5014d6b725cb584a1604743e4e878

# *******************  IMPORTING MODULES ********************

from neurosdk.scanner import Scanner
from neurosdk.cmn_types import *

import concurrent.futures
from time import sleep
import time
import numpy as np
import tensorflow as tf
import random
import threading as thread
from filters_lib import filters_sdk, filter_types
from nltk import flatten
from socket import *
import sys
import keyboard  # using module keyboard
import os
# *********************  G L O B A L S *********************
#file_csv = open (filePath,'a+')
sensor = None
resistanceOk = False
startSignal = False
header = 'timestamp;Pack;O1;O2;T3;T4;timestamp_ms;label\n'
all_signals = [-1,-1,-1,-1]
#all_signals_matrix =  [[-1 for x in range (12)] for y in range (1)]  

alpha = beta = delta = theta = gamma = [-1,-1,-1,-1]
all_waves = [-1,-1,-1,-1]
all_samples = []

sample_nr = 0
expected_samples = 2000                                         # there are 5 frequencies (alpa...gamma) and 4 sensors, if all 4 sensors are used
                                                                # this should be 5 x 4 = 20, the frequency is 10 Hz. 2 seconds of data with all
                                                                # 4 sensors = 2 * 5 * 4 * 10 = 400. 

confidence_threshold = 0.7                                      # default in Edge Impulse is 0.6
global isFailed
blinks = 0                                                      # amount of blinks
blinked = False                                                 # did you blink?
double_blinks = 0                                                      # amount of blinks
double_blinked = False                                                 # did you blink?
background = 0


stat=0          #A status value,ensure the mainloop() runs only once
tcpClientSocket = socket(AF_INET, SOCK_STREAM) #Set connection value for socket
#A global variable,for future socket connection
BUFSIZ=1024     #Set a buffer size
ip_stu=1        #Shows connection status

#Global variables of input status
BtnIP=''
ipaddr='192.168.1.51'
portApp = 10123
port2 = 10223
ipcon=0
send_pwm_conf = 1


row = 1

# Processed features (copy from Edge Impulse project)
features = [
  # <COPY FEATURES HERE!>
  #3 (Blink, Double_Blink, Noise)

]

flist = filters_sdk.FilterList()

#Filter definition
#High pass filter, let pass all frequencies above 0.3 Hz
hpf03 = filters_sdk.Filter()
hpf03.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 0.3 ))

#band stop filter, cutting at 50 Hz
bsf50 = filters_sdk.Filter()        
bsf50.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_band_stop, 250, 50))

#band stop filter, cutting at 60 Hz
bsf60 = filters_sdk.Filter()        
bsf60.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_band_stop, 250, 60))

#band stop filter, cutting at 60 Hz
bsf100 = filters_sdk.Filter()         
bsf100.init_by_param(filter_types.FilterParam(filter_types.FilterType. ft_band_stop, 250, 100))

#low pass filter, let pass all frequencies up 20 Hz 
lpf20 = filters_sdk.Filter() 
lpf20.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 20))
 

flist.add_filter(bsf60)
flist.add_filter(bsf50) 
flist.add_filter(lpf20)
flist.add_filter(hpf03)
flist.add_filter(bsf100)


# ==========================================================
# *******************  F U N C T I O N S *******************
# ==========================================================

# *********** Initiates TensorFlow Lite ***********
def initiate_tf():
    global interpreter, input_details, output_details

    ####################### TF Lite path and file ######################
    main_path = os.path.dirname(os.path.abspath(__file__))

    filePath = main_path + '\\Models\\v3\\'    
    lite_file = "ei-tfm-eeg-blinking-classifier-tensorflow-lite-float32v4-model.lite"  

    ####################### INITIALIZE TF Lite #########################
    # Load TFLite model and allocate tensors.
    interpreter = tf.lite.Interpreter(model_path = filePath + lite_file)

    # Get input and output tensors.
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Allocate tensors
    interpreter.allocate_tensors()

    # Printing input and output details for debug purposes in case anything is not working
    print()
    print("Input details:")
    print(input_details)
    print()
    print("Output details:")
    print(output_details)
    print()
    
 
# ****************** Init client  ******************
def init_tcp_client():
    #setPWM_threading=thread.Thread(target=sendTCPMessage)      #Define a thread for FPV and OpenCV
    #setPWM_threading.daemon = True                             #'True' means it is a front thread,it would close when the mainloop() closes
    #setPWM_threading.start()    
    with concurrent.futures.ThreadPoolExecutor() as executor:   
        future = executor.submit(sendTCPMessage)

  
def connect2Server():          #Call this function to connect with the server
    if ip_stu == 1:
        with concurrent.futures.ThreadPoolExecutor() as executor:   
                future = executor.submit(socket_connect)        
        #sc=thread.Thread(target=socket_connect) #Define a thread for connection
        #sc.daemon = True                     #'True' means it is a front thread,it would close when the mainloop() closes
        #sc.start()                              #Thread starts


def socket_connect():     #Call this function to connect with the server
    global ADDR,tcpClientSocket,BUFSIZ,ip_stu,ipaddr
    
    ip_adr=ipaddr       #Get the IP address from Entry
    SERVER_IP = ip_adr
    SERVER_PORT = 10123   #Define port serial 
    BUFSIZ = 1024         #Define buffer size
    ADDR = (SERVER_IP, SERVER_PORT)
    for i in range (1,6): #Try 5 times if disconnected
        try:
            if ip_stu == 1:
                print("Connecting to server @ %s:%d..." %(SERVER_IP, SERVER_PORT))
                print("Connecting")
                tcpClientSocket.connect(ADDR)        #Connection with the server
                print("Connected")
                                                   
                    
                ip_stu=0                         #'0' means connected
                    
                #at=thread.Thread(target=code_receive) #Define a thread for data receiving
                #at.daemon = True                   #'True' means it is a front thread,it would close when the mainloop() closes
                #at.start()                            #Thread starts
                break
            else:
                break
        except Exception:
            print("Cannot connecting to server,try it latter!")  
            print('Try %d/5 time(s)'%i)          
            ip_stu=1
            time.sleep(1)
            continue
    if ip_stu == 1:
        print('Disconnected')
    


# ****************** Reception from server  ******************
def code_receive():     #A function for data receiving
    global led_status,ipcon,findline_status,auto_status,opencv_status,speech_status
    while True:
        code_car = tcpClientSocket.recv(BUFSIZ) #Listening,and save the data in 'code_car'
        print(code_car)
        data = code_car.decode()
        print(data)
        
        
# ****************** Motor Control Thread  ******************
def sendTCPMessage():
    global send_pwm_conf, blinked, double_blinked
    #print('TCPMessage')
    message = ''
    while 1:
        try:        
            if blinked == True:
                # Send data
                #message = b'grab\n'                
                #print('sending lookRightStart')
                #time.sleep(0.3)                
                #tcpClientSocket.sendall(message)
                tcpClientSocket.send(('lookRightStart\n').encode())
                blinked = False
            
                
            elif double_blinked == True:
                # Send data
                #message = b'loose\n'
                #print('sending lookLeftStart')
                #time.sleep(0.3)
                #tcpClientSocket.sendall(message)
                tcpClientSocket.send(('lookLeftStart\n').encode())
                double_blinked = False
            
        except:
            print("Exception sending message")       
            
            if keyboard.is_pressed("space"):
                print("Exiting program\n")
                if sensor.is_supported_command(SensorCommand.StartResist):
                    sensor.exec_command(SensorCommand.CommandStopSignal)
                    print("Stop signal")                
                print("Exiting program\n")                     
            
        #time.sleep(0.2)
            
    
# ****************** EEG handlers START ******************

def sensor_found(scanner, sensors):
    for index in range(len(sensors)):
        print('Sensor found: %s' % sensors[index])


def on_sensor_state_changed(sensor, state):
    print('Sensor {0} is {1}'.format(sensor.name, state))


def on_battery_changed(sensor, battery):
    print('Battery: {0}'.format(battery))

def on_resist_received(sensor, data):
    global resistanceOk
    #BrainBitResistData(O1=inf, O2=inf, T3=inf, T4=inf)
    print(data)
    if(data.O1 and data.O2 and data.T3 and data.T4 < 1000000):
        print("Resistance ok. Experiment can start!")
        resistanceOk = True  
    else:
        resistanceOk = False

def on_signal_received(sensor, data):
    #print(data) 
    global row 
    global sample_nr, all_samples
    if(data != None):
        
    #'timestamp;Pack;O1;O2;T3;T4;timestamp_ms;label\n'
        for i  in range(0, len(data)):
                
            all_samples.insert(len(all_samples), flist.filter(data[i].O1)*100) 
            all_samples.insert(len(all_samples), flist.filter(data[i].O2)*100) 
            all_samples.insert(len(all_samples), flist.filter(data[i].T3)*100) 
            all_samples.insert(len(all_samples), flist.filter(data[i].T4)*100) 

            sample_nr += 4
            #print("sample_nr1: " + str(sample_nr) )
            if(sample_nr == 2000):
                break 
            
     
            
        if sample_nr >= expected_samples:               # Collected all samples...
            print("Got Data!")
            #print("Expected samples: " + str(expected_samples))
            all_samples = all_samples[:2000]  #limit samples to 2000
            #print("Size sample inference: " + str(len(all_samples)))
            #all_samples.append(all_signals_matrix)   
            #https://docs.edgeimpulse.com/docs/edge-impulse-studio/processing-blocks/flatten
            all_samples = flatten(all_samples)          # ...and flattening them
            inference()                                 # Inference function call 
            sample_nr = 0
            all_samples.clear()
            all_samples = []        





# ******** INFERENCE ******** 
def inference():
    global score, expected, choice, blinks, blinked, double_blinks, double_blinked

    #input_type = input_details[0]['dtype']  
    input_samples = np.array(all_samples, dtype=np.float32)
    # Add dimension to input sample (TFLite model expects (# samples, data))
    input_samples = np.expand_dims(input_samples, axis=0)

    # Create input tensor out of raw features
    # input_details[0]['index'] = the index which accepts the input
    interpreter.set_tensor(input_details[0]['index'], input_samples)
    #tensor_details = interpreter.get_tensor_details()

    # run the inference
    interpreter.invoke()

    # output_details[0]['index'] = the index which provides the input
    output_data = interpreter.get_tensor(output_details[0]['index'])

    # finding output data (Must fit order model on Edge Impulse) 
    blink           = output_data[0][0]
    double_blink    = output_data[0][1]
    noise           = output_data[0][2]
    

    # checking if over confidence threshold
    if blink >= confidence_threshold:
        choice = "Blink"
        blinks += 1
        blinked = True
        double_blinked = False
    elif noise >= confidence_threshold:
        choice = "Noise"
        blinked = False
        double_blinked = False
    elif double_blink >= confidence_threshold:
        choice = "Double_Blink"   
        double_blinks += 1
        double_blinked = True
        blinked = False     
    else:
        choice = "----"

    print(f"Blink:{blink:.4f} - Noise:{noise:.4f}  -  Double Blink:{double_blink:.4f}\n\n          ")
    
    
# ******** INFERENCE 2 ******** 
def inference2():
    global score, expected, choice, blinks, blinked, double_blinks, double_blinked
    global interpreter, input_details, output_details

    input_type = input_details[0]['dtype']  
    
    # Convert the feature list to a NumPy array of type float32
    np_features = np.array(all_samples, input_type)
    input_samples = np.expand_dims(np_features, axis=0)

    #print(len(input_data))
    #print("Input Data: " + str(input_data))
    interpreter.set_tensor(input_details[0]['index'], input_samples)

    interpreter.invoke()

    # The function `get_tensor()` returns a copy of the tensor data.
    # Use `tensor()` in order to get a pointer to the tensor.
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print(output_data)    

    # finding output data (Must fit order model on Edge Impulse) 
    blink           = output_data[0][0]
    double_blink    = output_data[0][1]
    noise           = output_data[0][2]
    

    # checking if over confidence threshold
    if blink >= confidence_threshold:
        choice = "Blink"
        blinks += 1
        blinked = True
    elif noise >= confidence_threshold:
        choice = "Noise"
    elif double_blink >= confidence_threshold:
        choice = "Double_Blink"   
        double_blinks += 1
        double_blinked = True     
    else:
        choice = "----"

    print(f"Blink:{blink:.4f} - Noise:{noise:.4f}  -  Double Blink:{double_blink:.4f}\n\n          ")
# ========================== Robot Control  ==============================    
    

# ******** Brainbit communication  ********

def int_sensor():
    sensor.sensorStateChanged = on_sensor_state_changed   
    
    #sensor.batteryChanged = on_battery_changed 
    
    if sensor.is_supported_feature(SensorFeature.Signal):
        sensor.signalDataReceived = on_signal_received                  

    if sensor.is_supported_feature(SensorFeature.Resist):
        sensor.resistDataReceived = on_resist_received
                    
    if sensor.is_supported_command(SensorCommand.StartResist):
        sensor.exec_command(SensorCommand.StartResist)
        print("Start resist\n")

def start_measuring_eeg():
    print("Enter start measuring EEG")
    if sensor.is_supported_command(SensorCommand.StopResist):
        sensor.exec_command(SensorCommand.StopResist)
        print("Stop resist\n")    
    else:
        print("StartResist not supported")            

    print("Start signal in 5 s\n")    
    sleep(1)
    print("Start signal in 4 s\n")    
    sleep(1)
    print("Start signal in 3 s\n")    
    sleep(1)
    print("Start signal in 2 s\n")    
    sleep(1)
    print("Start signal in 1 s\n")    
    sleep(1)
                
    if sensor.is_supported_command(SensorCommand.StartSignal):
        sensor.exec_command(SensorCommand.StartSignal)
        print("Start signal\n")                    
                    #init_program()
                    #sensor.exec_command(SensorCommand.StopSignal)
                    #print("Stop signal")
                    #save_data()
    else:
        print("StartSignal not supported")                 
        startSignal = True    
    
    init_program()

def init_program():
    initiate_tf()
    connect2Server()          #Call this function to connect with the server
    init_tcp_client()    

if __name__ == "__main__":
    
    try:
        scanner = Scanner([SensorFamily.LEBrainBit])

        scanner.sensorsChanged = sensor_found
        scanner.start()
        print("Starting search for 5 sec...")
        sleep(5)
        scanner.stop()

        sensorsInfo = scanner.sensors()
        for i in range(len(sensorsInfo)):
            current_sensor_info = sensorsInfo[i]
            print(sensorsInfo[i])


            def device_connection(sensor_info):
                return scanner.create_sensor(sensor_info)


            with concurrent.futures.ThreadPoolExecutor() as executor:
                future = executor.submit(device_connection, current_sensor_info)
                sensor = future.result()
                #init_program()
                print("Device connected")
                

            sensFamily = sensor.sens_family

            sensorState = sensor.state
            if sensorState == SensorState.StateInRange:
                print("connected")
            else:
                print("Disconnected")

            print(sensFamily)
            print(sensor.features)
            print(sensor.commands)
            print(sensor.parameters)
            print(sensor.name)
            print(sensor.state)
            print(sensor.address)
            print(sensor.serial_number)
            print(sensor.batt_power)
            
            if sensor.is_supported_parameter(SensorParameter.SamplingFrequency):
                print(sensor.sampling_frequency)
            if sensor.is_supported_parameter(SensorParameter.Gain):
                print(sensor.gain)
            if sensor.is_supported_parameter(SensorParameter.Offset):
                print(sensor.data_offset)
            print(sensor.version)

            
            with concurrent.futures.ThreadPoolExecutor() as executor:
                if sensor.is_supported_feature(SensorFeature.Signal):
                    sensor.signalDataReceived = on_signal_received                

                if sensor.is_supported_feature(SensorFeature.Resist):
                    sensor.resistDataReceived = on_resist_received
                
                if sensor.is_supported_command(SensorCommand.StartResist):
                    sensor.exec_command(SensorCommand.StartResist)
                    print("Start resist\n")
            
            # starting the Brainbit communication in separate thread
            #thread = thread.Thread(target=int_sensor)
            #thread.daemon = True
            #thread.start()                
           


#                sleep(5)
#                sensor.exec_command(SensorCommand.StopResist)
#                print("Stop resist")                

#            if sensor.is_supported_command(SensorCommand.StartSignal):
#                sensor.exec_command(SensorCommand.StartSignal)
#                print("Start signal")
#                sleep(5)
#                sensor.exec_command(SensorCommand.StopSignal)
#                print("Stop signal")

            if sensor == None:
                exit()

            #print("Resistance OK = " + str(resistanceOk)   +  " / " + "startSignal =" + str(startSignal))
            start_measuring_eeg()
            
        while True:

            '''
            if(resistanceOk == True and startSignal == False):
                if sensor.is_supported_command(SensorCommand.StartResist):
                    sensor.exec_command(SensorCommand.StopResist)
                    print("Stop resist\n")    
                else:
                    print("StartResist not supported")            

                print("Start signal in 5 s\n")    
                sleep(1)
                print("Start signal in 4 s\n")    
                sleep(1)
                print("Start signal in 3 s\n")    
                sleep(1)
                print("Start signal in 2 s\n")    
                sleep(1)
                print("Start signal in 1 s\n")    
                sleep(1)
                
                if sensor.is_supported_command(SensorCommand.StartSignal):
                    sensor.exec_command(SensorCommand.StartSignal)
                    print("Start signal\n")                    
                    #init_program()
                    #sensor.exec_command(SensorCommand.StopSignal)
                    #print("Stop signal")
                    #save_data()
                else:
                    print("StartSignal not supported") 
                
                startSignal = True
                                     
            '''                            
            if keyboard.is_pressed("space"):
                print("Exiting program\n")
                if sensor.is_supported_command(SensorCommand.StartResist):
                    sensor.exec_command(SensorCommand.CommandStopSignal)
                    print("Stop signal")                
                print("Exiting program\n")
                #save_data() #save all the data into different files before exit                
            #print("Running")
            
 
    
    
    except Exception as err:
        print(err)
        tcpClientSocket.close()

    del scanner    
    print('Remove scanner')   

