(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware reload stacktrace keyword-params params))
  (:use ring.util.response)
  (:use (hiccup core form-helpers))
  (:use (orc_battle_online game_logic html_rendering)))

(def foo-count (ref 0))

(defmulti handler :uri)

(defmethod handler "/main" [req]
	   {:status 200
	    :headers {"Content-type" "text/html"}
	    :body (render-game-html)})

(defmethod handler "/newgame" [req]
	   (init-monsters)
	   (init-player)
	   (handler (assoc req :uri "/main")))

(defmethod handler "/stab" [req]
	   {:status 200
	    :headers {"Content-type" "text/html"}
	    :body (html (show-monsters-html)
			(form-to [:post "/stab-monster"]
				 (label "stab-choice" "Which monster will you stab?")
				 (text-field "stab-choice")
				 (submit-button "Submit")))})

(defmethod handler "/stab-monster" [req]
	   (let [x (:stab-choice (:params req))]
	     (with-in-str (str "s\r\n" x "\r\n") (player-attack))
	     (redirect "/main")))

(defmethod handler "/foo" [req]
	   {:status 200
	    :headers {"Content-type" "text/html"}
	    :body (html
		   [:p "Page foo"
		    [:br]
		    [:a {:href "/"} "Go back"]])})

(defmethod handler "/bar" [req]
	   (handler (assoc req :uri "/foo")))

(defmethod handler "/" [req]
	   (dosync (alter foo-count inc))
	   {:status 404
	    :headers {"Content-type" "text/html"}
	    :body (html
		   [:h1 "Hello World from Ring and Hiccup!"]
		   [:a {:href "newgame"} "New Game"]
		   [:p (str "Count =" @foo-count)])})

(defmethod handler :default [req]
	   (handle-dump req))


;; let's see if we can add a ref hashmap and manipulate that through
;; form requests.  I wonder if the hashmap is stored in memory, so
;; we can have some kind of state

;; The refs do appear to be stored in memory, but if we do a
;; wrap-reload of the namespace they will be reloaded and so it will appear as though they don't do anything
(def app (-> handler
	     (wrap-keyword-params)
	     (wrap-params)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (run-jetty app {:port 8080}))

(defn -main [&args]
  (boot))