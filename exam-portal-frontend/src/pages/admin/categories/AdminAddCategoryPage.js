import React, { useState } from "react";
import "./AdminAddCategoryPage.css";
import { Button, Form } from "react-bootstrap";
import { useDispatch } from "react-redux";
import * as categoriesConstants from "../../../constants/categoriesConstants";
import FormContainer from "../../../components/FormContainer";
import RoleSidebar from "../../../components/RoleSidebar";
import {
  addCategory,
  fetchCategories,
} from "../../../actions/categoriesActions";
import swal from "sweetalert";
import { useNavigate } from "react-router-dom";

const AdminAddCategoryPage = () => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const submitHandler = (e) => {
    e.preventDefault();
    const category = { title: title, description: description };
    addCategory(dispatch, category).then((data) => {
      if (data.type === categoriesConstants.ADD_CATEGORY_SUCCESS) {
        swal("Class Added!", `${title} succesfully added`, "success");
        navigate("/adminCategories");
      } else {
        swal("Class Not Added!", data.payload || `${title} not added`, "error");
      }
      // navigate("/adminCategories");
    });
  };

  return (
    <div className="adminAddCategoryPage__container">
      <div className="adminAddCategoryPage__sidebar">
        <RoleSidebar />
      </div>
      <div className="mt-page">
        <FormContainer>
          <Button
            variant="secondary"
            className="mb-3"
            onClick={() => navigate("/adminCategories")}
          >
            ← Back to Classes
          </Button>
          <h2 style={{ color: "var(--mt-primary)" }}>Add Class</h2>
          <div className="mt-card p-4">
          <Form onSubmit={submitHandler}>
            <Form.Group className="my-3" controlId="title">
              <Form.Label>Title</Form.Label>
              <Form.Control
                type="text"
                placeholder="Enter Class Title"
                value={title}
                required
                onChange={(e) => {
                  setTitle(e.target.value);
                }}
              ></Form.Control>
            </Form.Group>

            <Form.Group className="my-3" controlId="description">
              <Form.Label>Description</Form.Label>
              <Form.Control
                style={{ textAlign: "top" }}
                as="textarea"
                rows="5"
                type="text"
                placeholder="Enter Category Description"
                value={description}
                onChange={(e) => {
                  setDescription(e.target.value);
                }}
              ></Form.Control>
            </Form.Group>

            <Button
              className="my-3 adminAddCategoryPage__content--button"
              type="submit"
              variant=""
            >
              Add
            </Button>
          </Form>
          </div>
        </FormContainer>
      </div>
    </div>
  );
};

export default AdminAddCategoryPage;
