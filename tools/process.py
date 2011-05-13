#!/usr/bin/env python
import sys
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

ALPHA = 0.8
gravity = [0,0,0]

f_now = open(sys.argv[1],"r")

values = {"timestamp":[], "latitude":[], "longitude":[], "altitude":[], 
        "bearing":[], "velocity":[], "gps_acc":[], "accelX":[], 
        "accelY":[], "accelZ":[],"accel_acc":[]}


for line in f_now:
    d_now =  line.strip().split(',')
    values['timestamp'].append(int(d_now[0]))
    values['latitude'].append(float(d_now[1]))
    values["longitude"].append(float(d_now[2]))
    values["altitude"].append(float(d_now[3]))
    values["bearing"].append(float(d_now[4]))
    values["velocity"].append(float(d_now[5]))
    values["gps_acc"].append(float(d_now[6]))
    values["accelX"].append(float(d_now[7]))
    values["accelY"].append(float(d_now[8]))
    values["accelZ"].append(float(d_now[9]))
    values["accel_acc"].append(int(d_now[10]))
    
f_now.close()

offset = values["timestamp"][0]
x = [ (val-offset)/1000 for val in values["timestamp"] ]

def __remove_gravity():
    accels = [[],[],[]]
    for i in range(len(x)):
        gravity[0] = ALPHA * gravity[0] + (1-ALPHA) * values["accelX"][i]
        gravity[1] = ALPHA * gravity[1] + (1-ALPHA) * values["accelY"][i]
        gravity[2] = ALPHA * gravity[2] + (1-ALPHA) * values["accelZ"][i]
        
        accels[0].append(values["accelX"][i] - gravity[0])
        accels[1].append(values["accelY"][i] - gravity[1])
        accels[2].append(values["accelZ"][i] - gravity[2])
    return accels
        

def plot_accelerometer():
    a_vals = __remove_gravity()
    plt.plot(x,a_vals[0],'r',label="x-axis")
    plt.plot(x,a_vals[1],'g', label="y-axis")
    plt.plot(x,a_vals[2],'b', label="z-axis")
    
    plt.legend()
    plt.title("Acceleration")
    plt.xlabel("seconds")
    plt.ylabel("m/s^2")
    
def plot_locations():
    fig = plt.figure()
    ax = fig.gca(projection='3d')    
    ax.plot(values["longitude"], values["latitude"], values["altitude"], label='Movements')
    plt.xlabel("longitude")
    plt.ylabel("latitude")
    
def plot_speed(unit="mph"):
    if unit == "mph":
        conv = 1601
    else:
        conv = 1000
    
    y = [ (val*3600)/conv for val in values["velocity"] ]
    plt.plot(x,y)
    plt.grid(True)
    plt.title("Velocity")
    plt.xlabel("seconds")
    plt.ylabel(unit)

def plot_route():
    plt.plot(values["longitude"], values["latitude"])
    
def plot_altitude():
    plt.plot(x, values["altitude"],label="Altitude")
    
def plot_bearing():
    plt.plot(x, values["bearing"], label="Bearing")
    

if sys.argv[2] == "a":
    plot_accelerometer()
elif sys.argv[2] == "l":
    plot_locations()
elif sys.argv[2] == "s": 
    plot_speed()
elif sys.argv[2] == "r":   
    plot_route()
elif sys.argv[2] == "h":
    plot_altitude()
elif sys.argv[2] == "b":
    plot_bearing()
    
if sys.argv[2] != "l":
    plt.grid(True)
plt.show()