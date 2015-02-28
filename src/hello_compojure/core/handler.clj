(ns hello-compojure.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [monger.core :as mg]
            [monger.collection :as mc])
  (:import [com.mongodb DB WriteConcern]
           [org.bson.types ObjectId]))
(use 'ring.util.anti-forgery)
(use 'hiccup.core)
(use 'ring.util.response)

;(let [conn (mg/connect)
;      db (mg/get-db conn "monger-test")]
;  (mc/remove db "documents"))

(defn rand-string [input n]
  (if (= n 0)
    input
    (str input
       (rand-string (.charAt "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" (rand-int 52)) (dec n)))))

(defn make-project-map [project-text]
   {:slug (rand-string "" 5) :text project-text})

(defn get-project [id]
  (let [conn (mg/connect)
        db (mg/get-db conn "monger-test")]
    (mc/find-one-as-map db "documents" {:slug id})))

(defn add-to-projects [project-text]
  (let [conn (mg/connect)
        db (mg/get-db conn "monger-test")
        project-map (make-project-map project-text)]
    (mc/insert db "documents" project-map)
    project-map))

(defn homepage-html [anti-forgery-field] 
  (html [:br]
    [:form {:action "/" :method "post" } 
      [:input {:type "text" :name "submitted-text" :autofocus ""}]
      anti-forgery-field
      [:input {:type "submit"}]]))

(defroutes app-routes
  (GET "/" [] 

       (let [conn (mg/connect)
             db (mg/get-db conn "monger-test")]
          (mc/find-maps db "documents"))

       (str (homepage-html (anti-forgery-field))))
  
  (POST "/" [submitted-text]
       (let [project-map (add-to-projects submitted-text)
             slug (get project-map :slug)]
         (redirect (str "/p/" slug))))
  
  (GET "/p/:id" [id]
       (str (get (get-project id) :text )))

  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
