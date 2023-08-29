VERSION = $(shell cat .release)
PREFIX ?= zimgraz
IMAGE = locust
IMAGE_NAME ?= $(IMAGE):$(VERSION)
FULL_IMAGE_TAG = "$(PREFIX)/$(IMAGE_NAME)"

all: pre-tag build

pre-tag:
	test $(VERSION)
	@echo "*** Release version: $(VERSION) ***"

build: pre-tag
	@echo "*** Building image $(FULL_IMAGE_TAG) ***"
	docker build -t $(FULL_IMAGE_TAG) ./

.PHONY: clean
clean: pre-tag
	@echo "*** Removing image $(FULL_IMAGE_TAG) ***"
	docker image rm $(FULL_IMAGE_TAG)

