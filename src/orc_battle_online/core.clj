(ns orc_battle_online.core
  (:use ring.adapter.jetty)
  (:use ring.handler.dump)
  (:use (ring.middleware flash reload stacktrace keyword-params params session))
  (:use ring.util.response)
  (:use (hiccup core form-helpers))
  (:use (orc_battle_online game_logic html_rendering)))

(defn response-html [body]
  (-> (response body)
      (content-type "text/html")))

(defmulti handler :uri)

(defmethod handler "/main" [req]
	   (response-html (str (render-game-html req))))

(defmethod handler "/newgame" [req]
	   (init-monsters)
	   (init-player)
	   (redirect "/main"))

(defmethod handler "/stab" [req]
	   (response-html (html (show-monsters-html)
			       (form-to [:post "/stab-monster"]
					(label "stab-choice" "Which monster will you stab?")
					(text-field "stab-choice")
					(submit-button "Submit")))))

; TODO add flash to spell out the result of the action
(defmethod handler "/stab-monster" [req]
	   (let [x (:stab-choice (:params req))]
	     (with-in-str (str "s\r\n" x "\r\n") (player-attack))
	     (-> (redirect "/main")
		 (assoc :session {:_flash (str "You stabbed monster " x)})
		 (assoc :flash "Success!"))))

(defmethod handler "/" [req]
	   (response-html (html [:h1 "Hello World from Ring and Hiccup!"]
				[:a {:href "newgame"} "New Game"])))

(defmethod handler :default [req]
	   (handle-dump req))


;; let's see if we can add a ref hashmap and manipulate that through
;; form requests.  I wonder if the hashmap is stored in memory, so
;; we can have some kind of state

(def app (-> handler
	     (wrap-session)
	     (wrap-keyword-params)
	     (wrap-params)
	     (wrap-flash)
	     (wrap-reload '(orc_battle_online.core))
	     (wrap-stacktrace)))

(defn boot []
  (run-jetty app {:port 8080}))

(defn -main [&args]
  (boot))