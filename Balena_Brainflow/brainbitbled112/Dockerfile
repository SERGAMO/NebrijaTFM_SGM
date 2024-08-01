# base-image for python on any machine using a template variable,
# see more about dockerfile templates here: https://www.balena.io/docs/learn/develop/dockerfile/
#FROM balenalib/raspberrypi3-python:3-stretch-run
FROM balenalib/raspberrypi3-ubuntu-python:3.10-lunar-build as build
#FROM balenalib/raspberrypi3-ubuntu-python:latest as build

# use `install_packages` if you need to install dependencies,
# for instance if you need git, just uncomment the line below.
#RUN install_packages git 

# Install build tools and remove layer cache afterwards
#RUN sudo add-apt-repository "deb http://security.ubuntu.com/ubuntu xenial-security main"

RUN apt-get -q update && apt-get install -yq --no-install-recommends \
	build-essential \
	#libfreetype6-dev \
	#libjpeg-dev \ 
	#i2c-tools \
	git \
	cmake 
	#python3-rpi.gpio
	#libjasper-dev \ 
	#libatlas-base-dev \
	#libgstreamer1.0-0 python3-smbus \
	#libqtgui4 \ 
	#libhdf5-dev \
	#libhdf5-serial-dev \ 
	#libqt4-test \
	#util-linux \
	#procps \ 
	#hostapd \
	#iproute2 \
	#iw \
	#haveged \
	#dnsmasq 


#RUN git clone https://github.com/oblique/create_ap 
#RUN cd ./create_ap && sudo make install


# Set our working directory
WORKDIR /usr/src/app
RUN git clone https://github.com/brainflow-dev/brainflow.git

RUN cd ./brainflow

RUN python -m pip install --upgrade pip --upgrade setuptools wheel interval-timer paho-mqtt influxdb
	#pip install --upgrade setuptools wheel \
	#pip install adafruit-circuitpython-pca9685 \
	#pip install adafruit-circuitpython-motor \
	#pip install RPi.GPIO \
	#pip install interval-timer \
	#pip install paho-mqtt \
	#pip install influxdb 	
	#pip install --upgrade luma.oled \
	#pip install --upgrade pip enum34 \
    #pip install rpi_ws281x \
    #pip install mpu6050-raspberrypi 
	#pip install cmake 
    #pip install flask \
    #pip install flask_cors \
    #pip install websockets 
    #pip install numpy \
    #
	#pip install opencv-contrib-python \
    #pip install imutils zmq pybase64 psutil
    
#RUN chmod 777 /home/pi/startup.sh
RUN python3 ./brainflow/tools/build.py
RUN cd ./brainflow/python_package &&  python3 -m pip install -U .

# pip install python deps from requirements.txt on the resin.io build server
#RUN pip install adafruit-circuitpython-sht31d \
#RUN		pip install RPi.GPIO \
# 		pip install paho-mqtt 



# This will copy all files in our root to the working  directory in the container
COPY . ./

# Enable udevd so that plugged dynamic hardware devices show up in our container.
ENV UDEV=1

# main.py will run when container starts up on the device
CMD ["python","-u","src/brainbitbled112.py"]