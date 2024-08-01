import argparse
import time
import os
from datetime import datetime
import numpy as np
import pandas as pd

from brainflow.board_shim import BoardShim, BrainFlowInputParams, BoardIds, BrainFlowPresets, LogLevels
from brainflow.data_filter import DataFilter




def main():
    

    BoardShim.enable_dev_board_logger()
    
    main_path = os.path.dirname(os.path.abspath(__file__))

    filePath = main_path + '\\file\\'
    filePath2 = main_path + '\\Blinks\\'
    fullPath = ""


    print("Creating CSV file...") 
    dateTimeObj = datetime.now()
    timestampStr = dateTimeObj.strftime("%Y-%m-%d %H_%M_%S.%f")    
    fullPath = filePath + timestampStr + ".csv" 
    
    
    params = BrainFlowInputParams()
    #params.serial_port = "/dev/ttyACM0"
    params.serial_port = "COM7"
    params.mac_address = "dd:e3:c3:ae:11:cb"

    
    #board = BoardShim(BoardIds.BRAINBIT_BOARD, params)
    board = BoardShim(BoardIds.BRAINBIT_BLED_BOARD.value, params)
    board.prepare_session()
    board.start_stream()
    BoardShim.log_message(LogLevels.LEVEL_INFO.value, 'start sleeping in the main thread')
    time.sleep(10)
    data = board.get_board_data()
    board.stop_stream()
    board.release_session()


    # demo how to convert it to pandas DF and plot data
    eeg_channels = BoardShim.get_eeg_channels(BoardIds.BRAINBIT_BLED_BOARD.value)
    print(eeg_channels)
    df = pd.DataFrame(np.transpose(data))
    print('Data From the Board')
    print(df.head(10))
    
  # demo for data serialization using brainflow API, we recommend to use it instead pandas.to_csv()
    DataFilter.write_file(data, fullPath, 'w')  # use 'a' for append mode
    restored_data = DataFilter.read_file(fullPath)
    restored_df = pd.DataFrame(np.transpose(restored_data))
    print('Data From the File')
    print(restored_df.head(10))

# e0:79:8d:73:bc:2e


if __name__ == "__main__":
    main()