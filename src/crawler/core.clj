(ns crawler.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]))

(def urls (agent {} :error-mode :continue :error-handler (fn [this_agent e] (.println System/out (.getMessage e)))))

(defn add-urls [new_urls]
  (doseq [url new_urls] (send-off urls assoc url {:visited? false :links '()})))


(defn file-to-strings [filename]
  (if (-> filename io/as-file .exists)
    (with-open [rdr (io/reader filename)] (doall (line-seq rdr)))
    '()))

(defn strings-to-file [filename strings]
  (with-open [wrtr (io/writer filename)]
    (doseq [string strings] (.write	wrtr (str string "\n")))))

(defn file-to-urls [filename]
  (-> filename file-to-strings add-urls))

(defn get-webpage [url]
  (try ((client/get url) :body) (catch Exception e (str))))

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

(defn find-links [webpage]
    (filter crawlable? (re-seq #"http://[^;\"' \t\n\r]+" webpage)))

(defn download-and-parse-url [url]
  (if ((second url) :visited?)
    (second url)
    {:visited? true :links (-> url first get-webpage find-links)}))


(defn clear-links [url_info]
  (if (url_info :visited?)
    (assoc url_info :links '())
    url_info))

(defn visit-url [urls_hm]
  (into {} (doall (pmap #(vector (first %) (download-and-parse-url %)) urls_hm))))

(defn rm-links-from-visited-urls [urls_hm]
  (into {} (doall (pmap #(vector (first %) (-> % second clear-links)) urls_hm))))

;;!!!!!error!!!!
(defn renew-urls []
  (->  (map #((second %) :links) (filter #((second %) :visited?) @urls )) flatten add-urls))
(map #((second %) :links) (filter #((second %) :visited?) @urls ))


(defn crawl-bunch [depth]
  (do
    (send-off urls visit-url)
    (await urls)
    (renew-urls)
    (await urls)
    ;(send-off urls rm-links-from-visited-urls)
    ;(await urls)
    (dec depth)
   )
  )

(defn save-visited-urls [filename]
  (strings-to-file filename (map #(first %) (filter  #((second %) :visited?) @urls))))

(defn save-found-urls [filename]
  (strings-to-file filename (map #(first %) @urls)))


(defn crawl [depth]
  (loop [i depth]
    (if (= i 0)
      (save-visited-urls "out_urls.txt")
      (recur (crawl-bunch i))
      )
    )
  )

;;тут типа мой репл
@urls
(file-to-urls "urls.txt")
;(restart-agent urls {})
(crawl 3)
(do
  (send-off urls visit-url)
  (await urls)
  )
(do
  (renew-urls)
  (await urls)
  )

(@urls "http://www.ucoz.ru/")
(@urls "http://fantet.narod.ru/sector/g101.htm")

(@urls "http://amisport.com.ua")
;(filter #((second %) :visited?) @urls )


(defn -main [& args]
  (do
    (file-to-urls "urls.txt")
    (crawl 2)))
