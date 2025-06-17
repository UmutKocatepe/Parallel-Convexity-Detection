import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ConvexKontrol {

    // Noktayı temsil eden sınıf (x, y koordinatları)
    public static class Point {
        double x, y;
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // koordinatlar.txt dosyasından noktaları okur
    public static List<Point> oku_koordinatlar(String dosyaAdi) {
        List<Point> noktalar = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(dosyaAdi))) {
            String satir;
            while ((satir = reader.readLine()) != null) {
                satir = satir.trim();
                if (!satir.isEmpty()) {
                    String[] parts = satir.split(",");
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    noktalar.add(new Point(x, y));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return noktalar;
    }

    // 3 nokta için cross product'ın z bileşenini hesaplar
    // Bu değer dönüş yönünü (sağa/sola) belirtir
    public static double cross_product_z(Point p1, Point p2, Point p3) {
        return (p2.x - p1.x) * (p3.y - p2.y) - (p2.y - p1.y) * (p3.x - p2.x);
    }

    // Seri convex kontrol algoritması
    public static boolean is_convex_serial(List<Point> points) {
        int n = points.size();
        if (n < 3) return false; // çokgen olabilmesi için en az 3 nokta gerekir

        List<Boolean> signs = new ArrayList<>();

        // Noktaları 3'lü gruplar halinde gez
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            Point p3 = points.get((i + 2) % n);
            double cp = cross_product_z(p1, p2, p3);

            // Dönüş yönü önemliyse işaretini kaydet
            if (cp != 0) {
                signs.add(cp > 0); // true = sola, false = sağa
            }
        }

        // Tüm işaretler aynıysa convex'tir
        for (Boolean sign : signs) {
            if (!sign.equals(signs.get(0))) {
                return false;
            }
        }
        return true;
    }

    // Thread sınıfı: her bir iş parçacığı bir üçlü nokta grubu işler
    public static class Worker extends Thread {
        List<Point> points;
        int index;
        List<Boolean> results;
        ReentrantLock lock;

        public Worker(List<Point> points, int index, List<Boolean> results, ReentrantLock lock) {
            this.points = points;
            this.index = index;
            this.results = results;
            this.lock = lock;
        }

        @Override
        public void run() {
            int n = points.size();
            Point p1 = points.get(index);
            Point p2 = points.get((index + 1) % n);
            Point p3 = points.get((index + 2) % n);

            double cp = cross_product_z(p1, p2, p3);

            // Dönüş yönünü hesapla ve sonuç listesine ekle (kilitleyerek)
            if (cp != 0) {
                lock.lock();
                try {
                    results.add(cp > 0);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    // Paralel convex kontrol algoritması
    public static boolean is_convex_parallel(List<Point> points) {
        int n = points.size();
        if (n < 3) return false;

        List<Boolean> results = Collections.synchronizedList(new ArrayList<>()); // paylaşımlı liste
        ReentrantLock lock = new ReentrantLock(); // thread-safe yazım için kilit
        List<Thread> threads = new ArrayList<>();

        // Her üçlü nokta için bir thread başlat
        for (int i = 0; i < n; i++) {
            Thread t = new Worker(points, i, results, lock);
            threads.add(t);
            t.start();
        }

        // Tüm thread'lerin tamamlanmasını bekle
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Sonuçlar içinde farklı dönüş yönü varsa: non-convex
        for (Boolean sign : results) {
            if (!sign.equals(results.get(0))) {
                return false;
            }
        }
        return true;
    }

    // Ana fonksiyon: noktaları oku, her iki kontrolü çalıştır, sonucu yazdır
    public static void main(String[] args) {
        String dosya = "koordinatlar.txt";
        List<Point> noktalar = oku_koordinatlar(dosya);

        System.out.println("Noktalar:");
        System.out.println("x    y");
        for (Point p : noktalar) {
            System.out.println(p.x + ", " + p.y);
        }

        boolean sonucSeri = is_convex_serial(noktalar);
        System.out.println("Seri Convex Kontrol: " + sonucSeri);

        boolean sonucParalel = is_convex_parallel(noktalar);
        System.out.println("Paralel Convex Kontrol: " + sonucParalel);
    }
}
