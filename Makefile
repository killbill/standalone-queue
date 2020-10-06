#
# Makefile to build apis for various bindings
#

define colorecho
        $(if $(TERM),
                @tput setaf $2
                @echo $1
                @tput sgr0,
                @echo $1)
endef

help: ## Show this help dialog.
	@echo
	$(call colorecho, "⁉️ Help", 5)
	@echo
	$(call colorecho, "$$(grep -hE '^\S+:.*##' $(MAKEFILE_LIST) | sed -e 's/:.*##\s*/:/' | column -c2 -t -s :)", 4)
	@echo

build_gen_go: ## Build Go api gen files
	@echo "Building gen GO apis files"
	@cd gen-go; ./build.sh

build_gen_java: ## Build Java api gen files
	@echo "Building gen JAVA apis files"
	@cd gen-java; ./build.sh
	@echo "Compiling and installing (.m2) java files"
	@cd gen-java/queue; mvn clean install

build_server: build_gen_java ## Building queue server
	@echo "Building queue server"
	@cd queue-server; mvn install

clean_gen_go: ## Cleanup Go api gen files
	@echo "Cleaning GO api files"
	@rm -Rf gen-go/api

clean_gen_java: ## Cleanup Java api gen files
	@echo "Cleaning Java apis files"
	rm -Rf "gen-java/queue/src/main/java/*";
	rm -Rf "gen-java/queue/target";

clean_server: ## Building queue server
	@echo "Building queue server"
	@cd queue-server; mvn clean

clean: clean_gen_go clean_gen_java  clean_server ## Cleanup all api gens & server

all: build_gen_go build_gen_java build_server ## Build all api gens & server

.PHONY: all build_gen_go build_gen_java build_server clean clean_gen_go clean_gen_java clean_server
               