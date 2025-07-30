import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditAdminMatchComponent } from './edit-admin-match.component';

describe('EditAdminMatchComponent', () => {
  let component: EditAdminMatchComponent;
  let fixture: ComponentFixture<EditAdminMatchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditAdminMatchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditAdminMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
