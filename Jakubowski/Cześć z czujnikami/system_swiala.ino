int czujnik =3; //CZUJNIK RUCHU
int czujnik2 =12; //CZUJNIK NATEZENIE SWIATŁA
int czujnik3 =5; //CZUJNIK WILGOCI
int wlacznik=2; //WŁACZNIK SYSTEMU SWIATEŁ
int wlacznik2=9; //WŁACZNIK SYSTEMU WYCIERACZEK
int val = 0;
void setup() {
 
pinMode(10, OUTPUT);
pinMode(6, OUTPUT);
pinMode(3, INPUT);
pinMode(12, INPUT);
pinMode(5, INPUT);
pinMode(8, OUTPUT);
pinMode(wlacznik, INPUT);
pinMode(wlacznik2, INPUT);
Serial.begin(9600);
}
void loop() {
  
/////////////////////////////PPRZEŁACZANIE ŚWIATEŁ Z DŁUGICH NA KROTKIE

digitalRead(czujnik);
val=digitalRead(czujnik); //do wyświetlania czujnika na szeregowym wyswietlaczu
 
Serial.println(val);
  
if ((digitalRead(czujnik)==1 || digitalRead(czujnik2)==0) && (digitalRead(wlacznik2) == HIGH) )
{
digitalWrite(6, HIGH);
delay(4000); // zwłoka w przypadku gdy czujniki przestaną wykrywac ruchu czy światło
}
 
else {
  
digitalWrite(6, LOW);

} 

/////////////////////////// WŁĄCZANIE WYCIERACZEK
if (digitalRead(wlacznik) == HIGH && digitalRead(czujnik3) == 0)
  {
   digitalWrite(8, HIGH);
  }
   
else {
   
   digitalWrite(8, LOW); 
  }

delay(300);
}
