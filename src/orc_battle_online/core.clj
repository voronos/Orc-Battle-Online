(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use (ring.middleware reload stacktrace))
  (:use hiccup.core))

(defmulti handler :uri)

(defmethod handler "/foo" [req]
	   {:status 200
	    :headers {"Content-type" "text/html"}
	    :body (html
		   [:p "Page foo"
		    [:br]
		    [:a {:href "/"} "Go back"]])})

(defmethod handler :default [req]
  {:status 404
   :headers {"Content-type" "text/html"}
   :body (html
	  [:h1 "Hello World from Ring and Hiccup!"]
	  [:p (str req)]
	  [:a {:href "foo"} "Page foo"])})


;; let's see if we can add a ref hashmap and manipulate that through
;; form requests.  I wonder if the hashmap is stored in memory, so
;; we can have some kind of state

(def app (-> #'handler
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))

(defn -main [&args]
  (boot))