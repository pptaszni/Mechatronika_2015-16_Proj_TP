#include<opencv\cv.h>
#include<opencv2\opencv.hpp>
#include<opencv\highgui.h>
#include<opencv2\features2d.hpp>
using namespace cv;

void MyAddaptiveThreshold(Mat input, Mat output, double l, double k)
{
	double mean = 0;
	double odchylenie = 0;
	double thresh;
	int licznik = 0; //Licznik zliczajacy ilosc pikseli o wartosci innej niz 0
	int i, j;

	//Petla obliczajaca srednia, omijajaca jednoczesnie piksele zerowe
	for (i = 0; i < input.rows;i++)
	{
		for (j = 0; j < input.cols; j++)
		{
			if (input.at<unsigned char>(i, j) != 0)
			{
				mean = mean + input.at<unsigned char>(i, j);
				licznik++;
			}
		}
	}
	if (licznik == 0) return;
	mean = mean / licznik;

	//Petla obliczajaca odchylenie standardowe, omijajaca jednoczesnie piksele zerowe
	for (i = 0; i < input.rows; i++)
	{
		for (j = 0; j < input.cols; j++)
		{
			if (input.at<unsigned char>(i, j) != 0) odchylenie = odchylenie + std::pow(mean - input.at<unsigned char>(i, j), 2);
		}
	}
	odchylenie = odchylenie / licznik;
	odchylenie = std::pow(odchylenie, 0.5);
	thresh = l*mean - k*odchylenie; //Obliczanie progu (l zwykle = 1)
	std::cout << mean << " " << odchylenie << " " << thresh << std::endl; //Pomocne by³o przy dobieraniu odpowiednich wspolczynnikow k i l

	//Petla progujaca
	for (i = 0; i < input.rows; i++)
	{
		for (j = 0; j < input.cols; j++)
		{
			if (input.at<unsigned char>(i, j)>thresh) output.at<unsigned char>(i, j) = 255;
			else output.at<unsigned char>(i, j) = 0;
		}
	}
	threshold(input, output, thresh, 255, THRESH_BINARY);
	
}


int main() 
{
	//Ustawienie detakcji blobow
	SimpleBlobDetector::Params params;
	params.filterByColor = true;
	params.blobColor = 255;
	params.filterByArea = true;
	params.minArea = 20;
	params.filterByConvexity = false;
	params.filterByCircularity = false;
	params.filterByInertia = false;
	Ptr<SimpleBlobDetector> detector=SimpleBlobDetector::create(params);
	std::vector<KeyPoint> keypoints;

	VideoCapture capture("avi.avi");
	if (!capture.isOpened()) {
		std::cout << "cannot read video!\n";
		return -1;
	}

	//Zmienne dotyczace rozmiarow obrazu
	int hight = capture.get(CAP_PROP_FRAME_HEIGHT);
	int width = capture.get(CAP_PROP_FRAME_WIDTH);
	//Potrzebne do obciecia obrazu od gory i dolu
	int hight_cut = hight * 0.3;
	int y_pos = hight * 0.45;

	//Matryce obrazow
	Mat frame(Size(width, hight_cut), 0);
	Mat frame2(Size(width, hight_cut), 0);
	Mat source(Size(width, hight), 0);
	Mat po1th(Size(width, hight_cut), 0);
	Mat pomth(Size(width, hight_cut), 0);

	//Okna
	namedWindow("ród³o", WINDOW_AUTOSIZE);
	namedWindow("Wynik");
	namedWindow("Obraz po niskim progowaniu");

	//Ustawienie odtwarzania
	capture.set(CV_CAP_PROP_FPS, 2);
	double rate = capture.get(CV_CAP_PROP_FPS);
	int delay = 1000 / rate;
	
	while (true)
	{
		if (!capture.read(source)) {
			break;
		}
		cvtColor(source, frame, CV_RGB2GRAY); //Zmiana na czrnobiale
		threshold(frame(Rect(0, y_pos, width, hight_cut)), po1th, 170, 255, THRESH_TOZERO); //Progowanie wstepne. Mial byc niski stopien, ale ze wzgledu na niski kontrast urzytej kamery jest wysoki.
		MyAddaptiveThreshold(po1th, pomth, 1, 0.3); //Progowanie adaptacyjne wg literatury
		detector->detect(pomth, keypoints); // Detekcja blobow
		drawKeypoints(pomth, keypoints, frame2, Scalar(0, 0, 255), DrawMatchesFlags::DRAW_RICH_KEYPOINTS); //Rysowanie blobow na obrazie
		
		//Wyswietlanie obrazow w oknach
		imshow("ród³o", source);
		imshow("Wynik", frame2);
		imshow("Obraz po niskim progowaniu", po1th);

		if (waitKey(delay) >= 0) break;
	}

	capture.release();
	return 0;
}