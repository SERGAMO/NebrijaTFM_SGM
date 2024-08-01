import csv
import os.path

#csv[row][0] = "Timestamp"
#csv[row][1] = "Numpack"
#csv[row][2] = "O1"
#csv[row][3] = "O2"
#csv[row][4] = "T3"
#csv[row][5] = "T4"
#csv[row][6] = "Timestamp_ms"
#csv[row][7] = 'label'

main_path = os.path.dirname(os.path.abspath(__file__))

filePath = main_path + '\\chunks\\'
filePath2 = main_path + '\\Blinks\\'
header = ""

csv_fields = ['Timestamp', 'Numpack', 'O1', 'O2', 'T3', 'T4', 'Timestamp_ms', 'Label' ]

csv_filename = '2024-03-06 23_03_06_BrainbitApp_Exp_Data.csv' 



csv_fields = ['Timestamp', 'Numpack', 'O1', 'O2', 'T3', 'T4', 'Timestamp_ms', 'Label' ]

#open(os.path.join(os.path.dirname(__file__), '..', 'file.txt'))

def write_chunk(exp, part, lines):
    global header
    with open(filePath + csv_filename  + '_part_'+ str(part) + '.'+ exp + '.csv', 'w') as f_out:    
        f_out.write(header)
        f_out.writelines(lines)
        
    

with open( filePath2 + csv_filename, "r") as f:
    count = 1
    experiment = 'Noise'
    header = f.readline()
    lines = []
    for line in f:
        #Start with Noise        
        if experiment == 'Noise':
            if str('Blink') in line:
                write_chunk(experiment, count, lines)
                count += 1
                lines = []
                experiment = 'Blink'
            elif str('Noise') in line:
                lines.append(line)
        elif experiment == 'Blink':
            if str('Double_Blink') in line:
            #if str('Noise') in line:
                write_chunk(experiment, count, lines)
                count += 1
                lines = []
                experiment = 'Double_Blink'
                #experiment = 'Noise'
            #Lines containing Blink
            elif str('Blink') in line:
                lines.append(line)
        elif experiment == 'Double_Blink':
            if str('Noise') in line:
                write_chunk(experiment, count, lines)
                count += 1
                lines = []
                experiment = 'Noise'
            #Lines containing Blink
            elif str('Double_Blink') in line:
                lines.append(line)                
    
