(ns crawler.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-http.client :as client]))

(def urls (agent {} :error-handler (fn [this-agent e] (.println System/out (.getMessage e)))))

(defn add-urls [new_urls]
  (doseq [url new_urls] (send-off urls assoc url {:visited? false :not-used? true :links '()})))

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
  (let [url-adress (first url)
        url-info (second url)]
    (if (url-info :visited?)
      url-info
      {:visited? true :not-used? true :links (-> url-adress get-webpage find-links)})))

(defn mark-visited-url-as-used [url]
  (let [url-adress (first url)
        url-info (second url)]
    (if (url-info :visited?)
      (assoc url-info :not-used? false)
      url-info)))

(defn apply-functions-to-url-hashmap [urls-hm f1 f2]
  (into {} (doall (pmap #(vector (f1 %) (f2 %)) urls-hm))))

(defn visit-url [urls-hm]
  (apply-functions-to-url-hashmap urls-hm first download-and-parse-url))

(defn mark-used-urls [urls-hm]
  (apply-functions-to-url-hashmap urls-hm first mark-visited-url-as-used))

(defn renew-urls []
  (-> (map #((second %) :links) (filter #(and ((second %) :not-used?) ((second %) :visited?)) @urls )) flatten add-urls))

(defn crawl-bunch [depth]
  (do
    (send-off urls visit-url)
    (await urls)
    (renew-urls)
    (await urls)
    (send-off urls mark-used-urls)
    (await urls)
    (dec depth)))

(defn save-visited-urls [filename]
  (strings-to-file filename (map #(first %) (filter  #((second %) :visited?) @urls))))

(defn save-found-urls [filename]
  (strings-to-file filename (map #(first %) @urls)))

(defn crawl [depth]
  (loop [i depth]
    (if (= i 0)
      (save-found-urls "out_urls.txt")
      (recur (crawl-bunch i)))))

(defn -main [& args]
  (time
   (do
    (file-to-urls "urls.txt")
    (crawl 1))))
