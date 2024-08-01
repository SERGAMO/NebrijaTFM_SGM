#####################################################################
#       Record Bluetooth Dta from BrainBit EEG Through Bluetooth    #
#####################################################################
#           Coded     : Sergio Garrido (2024)                       #
#####################################################################

#https://www.geeksforgeeks.org/how-to-create-a-new-thread-in-python/

# *******************  IMPORTING MODULES ********************
from datetime import datetime
from timeit import default_timer as timer

from neurosdk.scanner import Scanner
from neurosdk.cmn_types import *

import concurrent.futures
from time import sleep
import time
import os
import subprocess as sp
import keyboard  # using module keyboard
import csv
import threading
import CSV_Separator
from babel.numbers import format_number, format_decimal, format_compact_decimal, format_percent
from filters_lib import filters_sdk, filter_types


# *********************  G L O B A L S *********************

sensor = None
resistanceOk = False
startSignal = False

main_path = os.path.dirname(os.path.abspath(__file__))

filePath = main_path + '\\Blinks\\'
filePath2 = main_path + '\\Blinks\\'
fullPath = ""

#file_csv = open (filePath,'a+')
header = 'timestamp;Pack;O1;O2;T3;T4;timestamp_ms;label\n'
all_signals = [[-1,-1,-1,-1,-1,-1,-1,-1],[1]]
all_signals_matrix =  [[-1 for x in range (14)] for y in range (1)]  

alpha = beta = delta = theta  = [-1,-1,-1,-1,-1]

current_file = ''
current_event = 0

row = 0
all_signals_matrix[row][0] = "Timestamp"
all_signals_matrix[row][1] = "Numpack"
all_signals_matrix[row][2] = "O1"
all_signals_matrix[row][3] = "O2"
all_signals_matrix[row][4] = "T3"
all_signals_matrix[row][5] = "T4"
all_signals_matrix[row][6] = "O1_raw"
all_signals_matrix[row][7] = "O2_raw"
all_signals_matrix[row][8] = "T3_raw" 
all_signals_matrix[row][9] = "T4_raw"
all_signals_matrix[row][10] = "Timestamp_ms"
all_signals_matrix[row][11] = 'label'
all_signals_matrix[row][12] = "bipolar_left" 
all_signals_matrix[row][13] = "bipolar_right"
row = 1 

secs = 2
start = timer()
recording = False 

# You have 2 choices when recording EEG-data: 
# 1) Show an event to record for a predefined time, this is preferable as it creates files that directly can be uploaded to Edge Impulse
# 2) Record a long stream of EEG-data into one file that needs to be split into files recognizable by Edge Impulse
# if 1) then record_many should be True, if 2) then it should be False

record_many = True 
                                       
# Put the events to record in this dictionary within "" and after : the seconds
# This is used for the Blink Pong game
rec_dicta = { 
    "Noise"         : 2,
    "Blink"         : 2,
    "Double_Blink"  : 2
     
}  

# This is used for the Mind Reader app, uncomment below rows and comment all other rec_dict rows
rec_dict = {
     "Noise"      : 3,
     "Left"       : 3,   
     "Blink"      : 3, 
     "Right"      : 3,
     "Blink2"      : 3 
 }

# Yet another example  
# rec_dict = {
#     "Left"    : 2, 
#     "Right"   : 2, 
#     "Noise"   : 2
# }   


ev = list(rec_dict.items())[current_event][0]          
current_ev = ev 

flist = filters_sdk.FilterList()

#Filter definition
#High pass filter, let pass all frequencies above 0.3 Hz
hpf03 = filters_sdk.Filter()
hpf03.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 0.3 ))

#High pass filter, let pass all frequencies above 1 Hz
hpf1 = filters_sdk.Filter()
hpf1.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 1.0 ))

#band stop filter, cutting at 50 Hz
bsf50 = filters_sdk.Filter()        
bsf50.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_band_stop, 250, 50))

#band stop filter, cutting at 60 Hz
bsf60 = filters_sdk.Filter()        
bsf60.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_band_stop, 250, 60))

#band stop filter, cutting at 100 Hz
bsf100 = filters_sdk.Filter()         
bsf100.init_by_param(filter_types.FilterParam(filter_types.FilterType. ft_band_stop, 250, 100))

#low pass filter, let pass all frequencies up 20 Hz 
lpf20 = filters_sdk.Filter() 
lpf20.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 20))
 
#High pass filter, let pass all frequencies above 4 Hz
hpf4 = filters_sdk.Filter()
hpf4.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 4.0 )) 

#High pass filter, let pass all frequencies above 4 Hz
hpf6 = filters_sdk.Filter()
hpf6.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 6.0 )) 

#High pass filter, let pass all frequencies above 7 Hz
hpf7 = filters_sdk.Filter() 
hpf7.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_hp, 250, 7.0 ))

#low pass filter, let pass all frequencies up 40 Hz  
lpf40 = filters_sdk.Filter() 
lpf40.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 40))

#low pass filter, let pass all frequencies up 40 Hz  
lpf30 = filters_sdk.Filter() 
lpf30.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 30))

#low pass filter, let pass all frequencies up 20 Hz 
lpf1 = filters_sdk.Filter() 
lpf1.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 1))

#low pass filter, let pass all frequencies up 20 Hz  
lpf03 = filters_sdk.Filter() 
lpf03.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 0.3))
 
 #low pass filter, let pass all frequencies up 20 Hz  
lpf4 = filters_sdk.Filter() 
lpf4.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 4.0))

 #low pass filter, let pass all frequencies up 20 Hz  
lpf7 = filters_sdk.Filter() 
lpf7.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 7.0))

 #low pass filter, let pass all frequencies up 20 Hz  
lpf10 = filters_sdk.Filter() 
lpf10.init_by_param(filter_types.FilterParam(filter_types.FilterType.ft_lp, 250, 10.0))


 
 #Blinks 
'''
flist.add_filter(bsf60)
flist.add_filter(bsf50) 
flist.add_filter(lpf20) 
flist.add_filter(hpf03) 
flist.add_filter(bsf100)
'''
#Order counts!!!
#Curva hacia abajo
#flist.add_filter(bsf50)  
#flist.add_filter(lpf30) #más alto, menos ruido 6

#BEST FILTER COMBINATION ###########################
#flist.add_filter(bsf50)   
#flist.add_filter(lpf20) #más alto, menos ruido 
#flist.add_filter(hpf03)  
#####################################################

#**********Not BAD with bipolar**********
#Recommended Frequency Range:

#Mu (8-12 Hz): Desynchronization of Mu rhythms in the sensorimotor cortex is associated with motor preparation and execution. You might see a decrease in Mu power around C3/C4 (potentially O1/O2, T3/T4) when a person prepares to move their arm.
#flist.add_filter(bsf50)
#flist.add_filter(hpf03)
#flist.add_filter(lpf10)
#*************************

#Great for detecting left arm movement
#Maybe try to move diferent parts of the arm to see if there is a difference
'''
 
Here's what we can discuss for filtering brainwaves related to arm movement using a BrainBit with dry ele ctrodes on O1, O2, T3, and T4 (10-20 international system):
 
Brainwaves and Motor Movement:
 
Sensorimotor cortex activity, located around the central sulcus (C3/C4 in the 10-20 system), is primarily responsible for planning and executing movements.
While BrainBit electrodes (O1, O2, T3, T4) don't directly  cover C3/C4, they can still pick up some information related to arm movement due to their proximity and functional connectivity.
Recommended Frequency Range:

Mu (8-12 Hz): Desynchronization of Mu rhythms in the sensorimotor cortex is associated with motor preparation and execution. You might see a decrease in Mu power around C3/C4 (potentially O1/O2, T3/T4) when a person prepares to move their arm.
https://es.wikipedia.org/wiki/Ritmo_mu 
'''

#No se obtiene mala respuesta sólo con un pasabajos de 10, pero preferible hacer un pasabanda.
 
flist.add_filter(bsf60)
flist.add_filter(bsf50) 
flist.add_filter(lpf10) 
flist.add_filter(hpf03) 
flist.add_filter(bsf100) 


  
# ==========================================================
# *******************  F U N C T I O N S *******************
# ==========================================================

   
# ****************** EEG-handlers ******************

        
def sensor_found(scanner, sensors): 
    for index in range(len(sensors)):
        print('Sensor found: %s' % sensors[index])


def on_sensor_state_changed(sensor, state):
    print('Sensor {0} is {1}'.format(sensor.name, state))


def on_battery_changed(sensor, battery):
    print('Battery: {0}'.format(battery))


def on_signal_received(sensor, data):
    global current_ev, row
#Data example

        
    #print(data)
    if(data != None):
        
    #'timestamp;Pack;O1;O2;T3;T4;timestamp_ms;label\n'
        for i  in range(0, len(data)):
                

            filterO1 = flist.filter(data[i].O1)
            filterO2 = flist.filter(data[i].O2)
            filterT3 = flist.filter(data[i].T3)
            filterT4 = flist.filter(data[i].T4)
              
            
            all_signals_matrix.append([row, data[i].PackNum, filterO1, filterO2, filterT3, filterT4, 
                                       data[i].O1, data[i].O2, data[i].T3, data[i].T4, str(round(time.time() * 1000)), current_ev, filterT3-filterO1, filterT4-filterO2])
            #Increment fow position      
            row += 1
                
        save_csv_file() 
  

def on_resist_received(sensor, data):
    global resistanceOk
    #BrainBitResistData(O1=inf, O2=inf, T3=inf, T4=inf)
    print(data)
    if(data.O1 and data.O2 and data.T3 and data. T4 < 1000000):
        print("Resistance ok. Experiment can start!")
        resistanceOk = True

    else:
        resistanceOk = False



def on_mems_received(sensor, data):
    print(data)


def on_fpg_received(sensor, data):
    print(data)


def on_amp_received(sensor, data):
    print(data)
         
        
# ********* Showing one event at a time *********
def save_data():
    global fullPath
    print("Creating CSV file...") 
    dateTimeObj = datetime.now()
    timestampStr = dateTimeObj.strftime("%Y-%m-%d %H_%M_%S.%f")    
    fullPath = filePath + timestampStr +  "_filters.csv" 
    with open(filePath + timestampStr +  "_filters.csv", "w", newline='') as csvFile:
        csvWriter = csv.writer(csvFile, delimiter=';')
        csvWriter.writerows(all_signals_matrix)
        
    CSV_Separator.processCSV(timestampStr +  "_filters.csv")

    exit() 
      


# ********* Showing one event at a time *********
def save_csv_file():
    global current_event, current_file, current_ev
    global start, end, secs, row

    ev = list(rec_dict.items())[current_event][0] 
    
    end = timer()
    if (end - start) >= secs:                                       # if we've waited enough for the current event
        start = timer()                                             # getting current time
        ev = list(rec_dict.items())[current_event][0]                # fetching current event
        secs = list(rec_dict.items())[current_event][1]             # fetching seconds for current event
        
        ev = list(rec_dict.items())[current_event][0]  
        current_ev = ev



        print(f"***** Think:\t {ev}   \t\t{secs}  seconds ***** ")

        dict_length = len(rec_dict)                                 # how many events in the dictionary
        if current_event < dict_length-1:                           # if end not reached...
            current_event += 1                                      # ...increasing counter
        else:
            current_event = 0                                       # if end reached, starting over

if __name__ == "__main__":

    try:
        scanner = Scanner([SensorFamily.LEBrainBit])

        scanner.sensorsChanged = sensor_found
        scanner.start()
        print("Starting search for 10 sec...")
        sleep(10)
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
                print("Device connected")

            sensor.sensorStateChanged = on_sensor_state_changed
            #sensor.batteryChanged = on_battery_changed

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
#                sleep(5)
#                sensor.exec_command(SensorCommand.StopResist)
#                print("Stop resist")                

#            if sensor.is_supported_command(SensorCommand.StartSignal):
#                sensor.exec_command(SensorCommand.StartSignal)
#                print("Start signal")
#                sleep(5)
#                sensor.exec_command(SensorCommand.StopSignal)
#                print("Stop signal")


        while True:
            if sensor == None:
                exit()
            #tmp = sp.call('cls',shell=True)
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
                    #sensor.exec_command(SensorCommand.StopSignal)
                    #print("Stop signal")
                    #save_data()
                else:
                    print("StartSignal not supported") 
                
                startSignal = True
                                     
                            
            if keyboard.is_pressed("space"):
                save_data() #save all the data into different files before exit                
                print("Exiting program\n")
                if sensor.is_supported_command(SensorCommand.StartResist):
                    sensor.exec_command(SensorCommand.CommandStopSignal)
                    print("Stop signal")                
                print("Exiting program\n")
                #save_data() #save all the data into different files before exit                
            #print("Running")
            

    
    
    except Exception as err:
        print(err)

    del scanner    
    print('Remove scanner')    
    