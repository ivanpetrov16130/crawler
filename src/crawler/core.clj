;;Юзаю либу clj-http с гитхаба
(ns crawler.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]
            [pl.danieljanus.tagsoup :as parser]
            )
  )

;;глобальная переменная, агент-список урлов //работает
(def urls (agent {}))

;;добавляет в агент урлы из списка строк //работает
(defn add-urls [new_urls]
  (doseq [url new_urls] (send urls assoc url {:visited? false :webpage {} :links []} )))

;;открывает текстовый файл и создает по нему список строк //работает
(defn file-to-strings [filename]
  (if (.exists (io/as-file filename))
    (with-open [rdr (io/reader filename)] (doall (line-seq rdr)))
    '() ))
;(file-to-strings "urls.txt")

;;записывает список строк в фалй //работает
(defn strings-to-file [filename strings]
  (with-open [wrtr (io/writer filename)]
    (doseq [string strings] (.write	wrtr (str string "\n")))))
;(strings-to-file "1.txt" '("q" "w" "ertyu"))

;;заполняет агент урлами из файла
(defn file-to-urls [filename]
  (add-urls (file-to-strings "urls.txt")))

;;проверяет, можно ли пройти по урле
(defn crawlable? [url]
	(cond
		(re-matches #"(?i).*?\.css$" url) false
		(re-matches #"(?i).*?\.js$" url) false
		(re-matches #"(?i).*?\.gif$" url) false
		(re-matches #"(?i).*?\.jpg$" url) false
		(re-matches #"(?i).*?\.jpeg$" url) false
		(re-matches #"(?i).*?\.png$" url) false
		(re-matches #"(?i).*?\.mp3$" url) false
		(re-matches #"(?i).*?\.cgi$" url) false
		(re-matches #"(?i).*?\.exe$" url) false
		(re-matches #"(?i).*?\.gz$" url) false
		(re-matches #"(?i).*?\.swf$" url) false
		(re-matches #"(?i).*?\.dmg$" url) false
		(re-matches #"(?i).*?\.dtd$" url) false
		(re-matches #".*?\.\);$" url) false
		#(true) true))

;;из тела веб-страницы собирает в вектор все проходимые ссылки вида https://...  //работает
(defn find-links [webpage]
  (filter crawlable? (re-seq #"http://[^;\"' \t\n\r]+" webpage)))
;(find-links (get (client/get "http://www.wowlol.ru") :body))

;;на вход вектор ["http://site.com" {:visited? ... :webpage ... :links ...}]
;;если урлу не открывали, то качает веб-страницу по урле, достает из неё тело и в нём находит вектор с ссылками
;;на выходе {:visited? true :webpage {...} :links [...]} //работает
(defn download-and-parse-url [url]
  (if (= (get (second url) :visited?) true)
    (second url)
    (let [webpage (client/get (first url))
          links (find-links (get webpage :body))]
      {:visited? true :webpage webpage :links links})))
;(download-and-parse-url ["http://www.ya.ru" {:visited? false :webpage {} :links []}])

;;ну тут ты знаешь //работает
(defn modfn [hmap]
  (into
   {}
   (map
    #(vector
      (first %)
      {})
    hmap)))
;(filter #(= false  ((second %) :visited?) ) {:a {:visited? true :q 1} :b {:visited? false :q 2} :c {:visited? false :q 3}})

;;тут тоже самое,функция для работы внутри агента: мапаю скачивание веб страниц по каждой урле
(defn visit-url [urls_hm]
  (into
   {}
   (map
    #(vector
      (first %)
      (download-and-parse-url %))
    urls_hm)))

;;добавляет найденные в visit-url ссылки в агент //вроде работает, хотя целенаправленно не тестил
(defn renew-urls []
  (add-urls (flatten (map #(get (second %) :links) (filter #(get (second %) :visited?) @urls )))));new urls!!!!!!!

;;прокраулить пак страниц //вроде работает
(defn crawl-bunch [depth]
  (do
    (send urls visit-url)
    (renew-urls)
    (dec depth)
   )
  )

;;записать все урлы из агента в файл //не тестил
(defn save-visited-urls [filename]
  (strings-to-file filename (map #(first %) @urls))
  )

;;главная функция, инициирует скачивание паков страниц, пока не кончится счётчик //не тестил
(defn crawl [depth]
  (loop [i depth]
    (if (= i 0)
      (save-visited-urls "out_urls.txt")
      (recur (crawl-bunch i))
      )
    )
  )

;;тут типа мой репл
@urls;;вывожу содержимое агента
(file-to-urls "urls.txt");;заполняю агент из файла
(restart-agent urls {});;перезагружаю агент в случае сбоя
(send urls modfn) ;;вызов с твоей функцией работает
(send urls visit-url) ;;вызов с моей функцией нихуя не работает, ничего не меняет в агенте



(defn -main [& args]
  (do
    (crawl 3)
   ))
