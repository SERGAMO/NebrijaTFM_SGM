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

filePathChunks = main_path + '\\chunks\\'
filePathBlinks = main_path + '\\Blinks\\'
header = ""

csv_fields = ['Timestamp', 'Numpack', 'O1', 'O2', 'T3', 'T4', 'Timestamp_ms', 'Label' ]

csv_filename = '' 



#open(os.path.join(os.path.dirname(__file__), '..', 'file.txt'))

def write_chunk(exp, part, lines):
    global header, csv_filename
    with open(filePathChunks + csv_filename  + '_part_'+ str(part) + '.'+ exp + '.csv', 'w') as f_out:    
        f_out.write(header)
        f_out.writelines(lines)
        
def processCSV(arg1):
    global header, csv_filename    
    csv_filename = arg1
    print("CSV_Separator called with arg: " + arg1)
    with open( filePathBlinks + csv_filename, "r") as f:
        count = 1
        experiment = 'Noise'
        header = f.readline()
        lines = []
        for line in f:                                  
                          
            #Start with Noise                  
                                          
            if experiment == 'Noise':
                if str('Left') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Left'
                elif str('Noise') in line:
                    lines.append(line)
            elif experiment == 'Left':
                if str('Right') in line:
                #if str('Noise') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Right'
                    #experiment = 'Noise'
                #Lines containing Blink
                elif str('Left') in line:
                    lines.append(line)
            elif experiment == 'Right':
                if str('Noise') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Noise'
                #Lines containing Blink
                elif str('Right') in line:
                    lines.append(line) 
'''

            
            
            if experiment == 'Noise':
                if str('Left') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Left'
                elif str('Noise') in line:
                    lines.append(line)
            elif experiment == 'Left':
                if str('Right') in line:
                #if str('Noise') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Right'
                    #experiment = 'Noise'
                #Lines containing Left
                elif str('Left') in line:
                    lines.append(line)
            elif experiment == 'Right':
                if str('Noise') in line:
                    write_chunk(experiment, count, lines)
                    count += 1
                    lines = []
                    experiment = 'Noise'
                #Lines containing Left
                elif str('Right') in line:
                    lines.append(line)    
                    
'''                    