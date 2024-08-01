# base-image for python on any machine using a template variable,
# see more about dockerfile templates here: https://www.balena.io/docs/learn/develop/dockerfile/
#FROM balenalib/raspberrypi3-python:3-stretch-run
#FROM balenalib/raspberrypi3-ubuntu-python:3.7-bionic-build as build
#FROM balenalib/raspberrypi3-ubuntu-python:latest as build
#FROM balenalib/raspberrypi3-64-ubuntu-python:latest as build
FROM balenalib/raspberrypi4-64-ubuntu-python:latest as build


# use `install_packages` if you need to install dependencies,
# for instance if you need git, just uncomment the line below.
#RUN install_packages git 

# Install build tools and remove layer cache afterwards
#RUN sudo add-apt-repository "deb http://security.ubuntu.com/ubuntu xenial-security main"

ENV DEBIAN_FRONTEND noninteractive
RUN echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections

RUN apt-get -q update && apt-get install -yq --no-install-recommends \
	build-essential \
	libfreetype6-dev \
	libjpeg-dev \ 
	i2c-tools \
	#libjasper-dev \ 
	libatlas-base-dev \
	libgstreamer1.0-0 \
	#libqtgui4 \ 
	libhdf5-dev \
	libhdf5-serial-dev \ 
	#libqt4-test \
	util-linux \
	procps \ 
	hostapd \
	iproute2 \
	iw \
	haveged \
	dnsmasq \
	git-all


RUN git clone https://github.com/oblique/create_ap
RUN cd ./create_ap && sudo make install


# Set our working directory
WORKDIR /usr/src/app


RUN python -m pip install --upgrade setuptools wheel \
	pip install adafruit-circuitpython-pca9685 \
	pip install adafruit-circuitpython-motor \
	pip install RPi.GPIO \
 	pip install paho-mqtt \
	pip install --upgrade luma.oled \
	pip install --upgrade pip enum34 \
    pip install rpi_ws281x \
    pip install mpu6050-raspberrypi \
	pip install smbus \
    #pip install flask \
    #pip install flask_cors \
    pip install websockets \
    #pip install numpy \
    #
	#pip install opencv-contrib-python \
	#pip install opencv-contrib-python-headless \
	#pip install opencv-python-headless \
    pip install imutils zmq pybase64 psutil
    
#RUN chmod 777 /home/pi/startup.sh
	

# pip install python deps from requirements.txt on the resin.io build server
#RUN pip install adafruit-circuitpython-sht31d \
#RUN		pip install RPi.GPIO \
# 		pip install paho-mqtt 



# This will copy all files in our root to the working  directory in the container
COPY . ./

# Enable udevd so that plugged dynamic hardware devices show up in our container.
ENV UDEV=1

# main.py will run when container starts up on the device
CMD ["python3","-u","rasptank/server/appserverAP.py"]
#CMD ["python3","-u","rasptank/server/serverTFM.py"]