#!/usr/bin/env python
import sys
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

ALPHA = 0.8
gravity = [0,0,0]

f_now = open(sys.argv[1],"r")

data = []
headers = ("timestamp","latitude","longitude","altitude","bearing","velocity","gps_acc","accelX","accelY","accelZ","accel_acc")

for line in f_now:
    d_now =  line.strip().split(',')
    bit = {}
    for x in range(len(headers)):
        bit[headers[x]] = d_now[x]
    data.append(bit)
f_now.close()

offset = int(data[0]["timestamp"])
x = [ (int(val['timestamp'])-offset)/1000 for val in data]

def __remove_gravity(vals):
    accels = [[],[],[]]
    for val in vals:
        gravity[0] = ALPHA * gravity[0] + (1-ALPHA) * float(val['accelX'])
        gravity[1] = ALPHA * gravity[1] + (1-ALPHA) * float(val['accelY'])
        gravity[2] = ALPHA * gravity[2] + (1-ALPHA) * float(val['accelZ'])
        
        accels[0].append(float(val['accelX']) - gravity[0])
        accels[1].append(float(val['accelY']) - gravity[1])
        accels[2].append(float(val['accelZ']) - gravity[2])
    return accels
        

def plot_accelerometer():
    a_vals = __remove_gravity(data)
    plt.plot(x,a_vals[0],'r',label="x-axis")
    plt.plot(x,a_vals[1],'g', label="y-axis")
    plt.plot(x,a_vals[2],'b', label="z-axis")
    plt.grid(True)
    plt.legend()
    plt.title("Acceleration")
    plt.xlabel("seconds")
    plt.ylabel("m/s^2")
    plt.show()
    
def plot_locations():
    fig = plt.figure()
    ax = fig.gca(projection='3d')
    x = [float(val['longitude']) for val in data]
    y = [float(val['latitude']) for val in data]
    z = [float(val['altitude']) for val in data]
    
    ax.plot(x, y, z, label='Movements')
    plt.xlabel("longitude")
    plt.ylabel("latitude")
    plt.show()
    
def plot_speed(unit="mph"):
    if unit == "mph":
        conv = 1601
    else:
        conv = 1000
    
    y = [(float(val['velocity'])*3600)/conv for val in data]    
    plt.plot(x,y)
    plt.grid(True)
    plt.title("Velocity")
    plt.xlabel("seconds")
    plt.ylabel(unit)
    plt.show()

def plot_route():
    x = [float(val['longitude']) for val in data]
    y = [float(val['latitude']) for val in data]
    plt.plot(x,y)
    plt.show()


if sys.argv[2] == "a":
    plot_accelerometer()
elif sys.argv[2] == "l":
    plot_locations()
elif sys.argv[2] == "s": 
    plot_speed()
elif sys.argv[2] == "r":   
    plot_route()