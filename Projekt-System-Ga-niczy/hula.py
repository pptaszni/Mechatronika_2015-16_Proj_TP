#Podelismy decyzje, ze bardzo na reke nam by bylo zaliczenie na podstawie filmu z powodu wielu zaliczeń, tak wiec wybieramy ta opcje.
#Program dziala poprawnie, lecz tak jak wspomniano w filmie zasilacz niestety nie jest w stanie wszystkiego zasilic na raz.
import RPi.GPIO as GPIO
import time

#importy wyswietlacz
import Adafruit_Nokia_LCD as LCD
import Adafruit_GPIO.SPI as SPI

#import plotly.plotly as py
#py.sign_in('maxtreaming','3ne48sojpw')

from PIL import Image
from PIL import ImageDraw
from PIL import ImageFont

import numpy as np
import matplotlib.pyplot as plt

#importy czujnika temp
from w1thermsensor import W1ThermSensor



GPIO.setwarnings(False)


#PINY
dioda=20
pwmPin=14
dym=21
wypelnienie=50
czestotliwoscPWM=1000
twar=25
tmax=26
#CZAS STARTU
hour=float(time.strftime("%H"))
minute=float(time.strftime("%M"))
second=float(time.strftime("%S"))
global starttime
starttime=(hour*3600)+(minute*60)+second
global fullfate
fulldate=time.strftime("%H:%M:%S")

#SPI dla wyswietlacza
#SCLK = 4
SCLK = 2
DIN = 17
DC = 23
RST = 24
CS = 8

#ONE WIRE-init czujnika
sensor = W1ThermSensor()


#servo
SERVO=20


GPIO.setmode(GPIO.BCM)
GPIO.setup(dioda,GPIO.OUT) #pin diody na out
GPIO.setup(pwmPin,GPIO.OUT) #pin PWM na wyjscie
GPIO.setup(SERVO,GPIO.OUT)
GPIO.setup(dym,GPIO.IN,pull_up_down=GPIO.PUD_UP)     #pin dymu na wejscie


GPIO.output(dioda,GPIO.LOW)


servoczest=50
servo=GPIO.PWM(SERVO, servoczest)
servo.ChangeDutyCycle(10)
pwm=GPIO.PWM(pwmPin, czestotliwoscPWM)

servo.start(5)

def writecsv(nazwa,temp):
    plik = open(nazwa + ".csv", 'a')
    hournow=float(time.strftime("%H"))
    minutenow=float(time.strftime("%M"))
    secondnow=float(time.strftime("%S"))
    timenow=(hournow*3600)+(minutenow*60)+secondnow
    csvtime=timenow-starttime
    print("czas od poczatku:")
    print (csvtime)
    buff = str(str(csvtime) + "," + str(temp) ) +'\n'
    plik.write(buff)
    plik.close
    return 0

def obraz(filename):
    czas=[0]
    temperatura=[0]
    tab=[0]
    plik = open(filename + ".csv", 'r')
    while True:
        temp=plik.readline()
        if not temp: break
        tab=temp.split(',')
        czas.append(float(tab[0]))
        temperatura.append(float(tab[1]))
    plt.plot(czas,temperatura, 'ro')
    plt.suptitle('Zmiany temperatury w komorze silnika')
    plt.ylabel('Temperatura [oC]')
    plt.xlabel('Czas [s]')
    plt.show()
    plt.savefig(filename + ".png")
    print("zapis")
    
    
    
def buzzer(ileRazy):
    for j in range(0,ileRazy):
        for i in range(0,30):
            pwm.ChangeFrequency(czestotliwoscPWM+50*i)
            GPIO.output(dioda,GPIO.HIGH)
            time.sleep(0.1)
            GPIO.output(dioda,GPIO.LOW)
    pwm.stop()
    GPIO.output(dioda,GPIO.LOW)
    
def alarm(channel):
    servo.changedutycycle(10)
    while(GPIO.input(dym)==0):  #leci az zniknie dym
        txtprint("ALARM, DYM!")
        pwm.start(wypelnienie)
        buzzer(1)
        print ("alarm")
    txtprint("")

def alarmt():
    servo.changedutycycle(10)
    t=sensor.get_temperature()
  #  while(t>tmax):
    txtprint("ALARM!" + str(t) +" C")
    pwm.start(wypelnienie)
    buzzer(1)
    print ("alarmt")
    t=sensor.get_temperature()


def txtprint(text):
    # Software SPI usage (defaults to bit-bang SPI interface):
    disp = LCD.PCD8544(DC, RST, SCLK, DIN, CS)
    # Initialize library.
    disp.begin(contrast=60)   
    # Clear display.
    disp.clear()
    disp.display()
    # Make sure to create image with mode '1' for 1-bit color.
    image = Image.new('1', (LCD.LCDWIDTH, LCD.LCDHEIGHT))
    # Get drawing object to draw on image.
    draw = ImageDraw.Draw(image)
    # Draw a white filled box to clear the image.
    draw.rectangle((0,0,LCD.LCDWIDTH,LCD.LCDHEIGHT), outline=255, fill=255)
    # Load default font.
    font = ImageFont.load_default()
    # Write some text.
    draw.text((0,15),text, font=font)
    # Display image.
    disp.image(image)
    disp.display()

GPIO.add_event_detect(dym, GPIO.RISING, callback=alarm, bouncetime=100)
    
i=0
while(1):
    i=i+1
    time.sleep(1)
    temperature_in_celsius = sensor.get_temperature()
    temp = writecsv(fulldate,temperature_in_celsius)
    txtprint("TEMP:" + str(temperature_in_celsius) + " C")
    print(temperature_in_celsius)
    if(temperature_in_celsius>tmax):
        alarmt()
    elif(temperature_in_celsius>twar):
        txtprint("UWAGA!T:" + str(temperature_in_celsius) + " C")
    if(i%10==0):
        obraz(fulldate)
        
GPIO.cleanup()




