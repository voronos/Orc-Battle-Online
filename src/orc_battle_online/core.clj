(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware reload stacktrace keyword-params params))
  (:use hiccup.core))

(def foo-count (ref 0))

(defmulti handler :uri)

(defmethod handler "/foo" [req]
	   {:status 200
	    :headers {"Content-type" "text/html"}
	    :body (html
		   [:p "Page foo"
		    [:br]
		    [:a {:href "/"} "Go back"]])})

(defmethod handler "/" [req]
	   (dosync (alter foo-count inc))
	   {:status 404
	    :headers {"Content-type" "text/html"}
	    :body (html
		   [:h1 "Hello World from Ring and Hiccup!"]
		   [:p (str req)]
		   [:a {:href "foo"} "Page foo"]
		   [:p (str "Count =" @foo-count)])})

(defmethod handler :default [req]
	   (handle-dump req))


;; let's see if we can add a ref hashmap and manipulate that through
;; form requests.  I wonder if the hashmap is stored in memory, so
;; we can have some kind of state

;; The refs do appear to be stored in memory, but if we do a
;; wrap-reload of the namespace they will be reloaded and so it will appear as though they don't do anything
(def app (-> handler
	     (wrap-params)
	     (wrap-keyword-params)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (run-jetty app {:port 8080}))

(defn -main [&args]
  (boot))