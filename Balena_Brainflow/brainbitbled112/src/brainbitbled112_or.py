import argparse
import time
import os
from datetime import datetime
from influxdb import InfluxDBClient
from brainflow.data_filter import DataFilter, FilterTypes, AggOperations, NoiseTypes, DetrendOperations
import logging
from brainflow.board_shim import BoardShim, BrainFlowInputParams, BoardIds, BrainFlowPresets, BrainFlowError, BrainFlowExitCodes, LogLevels
import csv
from interval_timer import IntervalTimer

PACKAGE = 0
EEG_O1 = 3
EEG_O2 = 4
EEG_T3 = 1
EEG_T4 = 2 
BATTERY = 5
TIMESTAMP = 6
MARKER = 7


def main():
    
    influx_client = InfluxDBClient('localhost', 8086, database='balena')
    #influx_client.create_database('eeg_data')
    measurements = [
                {
                    'measurement': 'EEG_O1',
                    'fields': {
                        'value': float(2.3556)
                    }
                }
            ]

    measurements.extend([
                {
                    'measurement': 'EEG_O2',
                    'fields': {
                        'value': float(3.234)
                    }
                }
            ])

    measurements.extend([
                {
                    'measurement': 'EEG_T3',
                    'fields': {
                        'value': float(4.3243)
                    }
                }
            ])       
    measurements.extend([
                {
                    'measurement': 'EEG_T4',
                    'fields': {
                        'value': float(6.434)
                    }
                }
            ])                  
            
    influx_client.write_points(measurements)         

    BoardShim.enable_dev_board_logger()
    logging.basicConfig(level=logging.DEBUG)   

    params = BrainFlowInputParams()
    params.serial_port = "/dev/ttyACM0"
    params.mac_address = "dd:e3:c3:ae:11:cb"


    try:    
        board = BoardShim(BoardIds.BRAINBIT_BLED_BOARD.value, params)
        print(BoardShim.get_board_descr(BoardIds.BRAINBIT_BLED_BOARD.value, BrainFlowPresets.DEFAULT_PRESET))
        sampling_rate =board.get_sampling_rate(BoardIds.BRAINBIT_BLED_BOARD.value)

        all_signals_matrix =  [[-1 for x in range (12)] for y in range (1)]  
        
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
        row = 1     
        #{'battery_channel': 5, 'eeg_channels': [1, 2, 3, 4], 'eeg_names': 'T3,T4,O1,O2', 'marker_channel': 7, 'name': 'BrainBitBLED', 'num_rows': 8, 'package_num_channel': 0, 'sampling_rate': 250, 'timestamp_channel': 6}
        #{'battery_channel': 9, 'eeg_channels': [1, 2, 3, 4], 'eeg_names': 'T3,T4,O1,O2', 'marker_channel': 11, 'name': 'BrainBit', 'num_rows': 12, 'package_num_channel': 0, 'resistance_channels': [5, 6, 7, 8], 'sampling_rate': 250, 'timestamp_channel': 10}
        board.prepare_session()

        # its a little tricky and unclear, need to call start_stream to create data acquisition thread(it also sends CommandStartSignal)
        board.start_stream ()
        BoardShim.log_message(LogLevels.LEVEL_INFO.value, 'start sleeping in the main thread')
        time.sleep(20)
        # data = board.get_current_board_data (256) # get latest 256 packages or less, doesnt remove them from internal buffer
        data = board.get_board_data()  # get all data and remove it from internal buffer
        board.stop_stream()
        board.release_session()
        BoardShim.release_all_sessions()
        
        eeg_channels = BoardShim.get_eeg_channels(BoardIds.BRAINBIT_BLED_BOARD.value)
        sampling_rate = board.get_sampling_rate( board.get_board_id())        

        for count, channel in enumerate(eeg_channels):
            DataFilter.perform_bandstop(data[channel], sampling_rate, 48.0, 52.0, 3, FilterTypes.BUTTERWORTH_ZERO_PHASE, 0.0)   
            DataFilter.perform_bandpass(data[channel], sampling_rate, 3.0, 25.0, 2, FilterTypes.BUTTERWORTH_ZERO_PHASE, 0)             
            #DataFilter.perform_bandpass(data[channel], sampling_rate, 1.0, 48.0, 4, FilterTypes.BESSEL_ZERO_PHASE, 0)
            
            
            
        current_ev = 'None'
        #len(data) siempre da 8, que son las columnas
        #Aplica el filtro en todo el array de la señal
        print(len(data[0]))
        for i  in range(0, len(data[0])):
            all_signals_matrix.append([row, 
                                    data[PACKAGE,i], 
                                    0,#DataFilter.perform_bandstop(data[EEG_O1,i], board.get_board_id(), 48.0, 52.0, 3, FilterTypes.BESSEL, 0.0),
                                    0,#DataFilter.perform_bandstop(data[EEG_O2,i], board.get_board_id(), 48.0, 52.0, 3, FilterTypes.BESSEL, 0.0),
                                    0,#DataFilter.perform_bandstop(data[EEG_T3,i], board.get_board_id(), 48.0, 52.0, 3, FilterTypes.BESSEL, 0.0),
                                    0,#DataFilter.perform_bandstop(data[EEG_T4,i], board.get_board_id(), 48.0, 52.0, 3, FilterTypes.BESSEL, 0.0),
                                    data[EEG_O1,i]/1000000, 
                                    data[EEG_O2,i]/1000000, 
                                    data[EEG_T3,i]/1000000, 
                                    data[EEG_T4,i]/1000000, 
                                    data[TIMESTAMP,i], 
                                    current_ev])    
            measurements = [
                {
                    'measurement': 'EEG_O1',
                    'fields': {
                        'value': float(data[EEG_O1,i]/1000000)
                    }
                }
            ]

            measurements.extend([
                {
                    'measurement': 'EEG_O2',
                    'fields': {
                        'value': float(data[EEG_O2,i]/1000000)
                    }
                }
            ])

            measurements.extend([
                {
                    'measurement': 'EEG_T3',
                    'fields': {
                        'value': float(data[EEG_T3,i]/1000000)
                    }
                }
            ])       
            measurements.extend([
                {
                    'measurement': 'EEG_T4',
                    'fields': {
                        'value': float(data[EEG_T4,i]/1000000)
                    }
                }
            ])                  
            
            influx_client.write_points(measurements)     
            row += 1    
            
            #print("PACKAGE: " + str(data[PACKAGE,i])) 
            #print("EEG_O1: " + str(data[EEG_O1,i]/1000000)) 
            #print("EEG_O2: " + str(data[EEG_O2,i]/1000000))
            #print("EEG_T3: " + str(data[EEG_T3,i]/1000000))
            #print("EEG_T4: " + str(data[EEG_T4,i]/1000000))
            #print("TS: " + str(data[TIMESTAMP,i]))
            #print("MARKER: " + str(data[MARKER,i]))
            #print("BATT: " + str(data[BATTERY,i]))
        
        print("Creating CSV file...") 
        dateTimeObj = datetime.now()
        timestampStr = dateTimeObj.strftime("%Y-%m-%d %H_%M_%S.%f")    
        with open( timestampStr +  "_filters.csv", "w", newline='') as csvFile:
            csvWriter = csv.writer(csvFile, delimiter=';')
            csvWriter.writerows(all_signals_matrix)            
            
    except BrainFlowError as err:
        print(err)

# e0:79:8d:73:bc:2e


if __name__ == "__main__":
    main()